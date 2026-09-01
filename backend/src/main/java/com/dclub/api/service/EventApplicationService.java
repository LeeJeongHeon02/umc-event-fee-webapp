package com.dclub.api.service;

import com.dclub.api.api.ApiDtos.*;
import com.dclub.api.common.ApiException;
import com.dclub.api.domain.*;
import com.dclub.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class EventApplicationService {
    private final ClubEventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final PaymentObligationRepository paymentRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final CurrentMemberProvider currentMemberProvider;
    private final ApiMapper mapper;
    private final Clock clock;

    public EventApplicationService(ClubEventRepository eventRepository,
                                   ParticipationRepository participationRepository,
                                   PaymentObligationRepository paymentRepository,
                                   PaymentStatusHistoryRepository historyRepository,
                                   CurrentMemberProvider currentMemberProvider, ApiMapper mapper, Clock clock) {
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.currentMemberProvider = currentMemberProvider;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<EventListItem> getEvents() {
        Long memberId = currentMemberProvider.current().getId();
        List<EventListItem> items = eventRepository.findAllByStatusOrderByStartsAtAsc(EventStatus.PUBLISHED).stream()
                .map(event -> listItem(event, memberId)).toList();
        return PageResponse.of(items);
    }

    @Transactional(readOnly = true)
    public EventDetail getEvent(long eventId) {
        var member = currentMemberProvider.current();
        var event = findEvent(eventId);
        var participation = participationRepository.findByEventIdAndMemberId(eventId, member.getId()).orElse(null);
        var payment = paymentRepository.findByMemberIdAndSourceTypeAndSourceId(member.getId(), PaymentSourceType.EVENT, eventId).orElse(null);
        long joinedCount = joinedCount(eventId);
        boolean joined = participation != null && participation.getStatus() == ParticipationStatus.JOINED;
        boolean canJoin = !joined && event.getStatus() == EventStatus.PUBLISHED
                && !Instant.now(clock).isAfter(event.getRegistrationDeadline())
                && (event.getCapacity() == null || joinedCount < event.getCapacity());
        return new EventDetail(event.getId(), event.getTitle(), event.getSummary(), event.getDescription(),
                event.getLocation(), event.getStartsAt(), event.getEndsAt(), event.getRegistrationDeadline(),
                event.getCapacity(), joinedCount, event.getFeeAmount(), event.getStatus(),
                participation == null ? null : participation.getStatus(), payment == null ? null : payment.getStatus(),
                event.isAllowLateCancellation(), canJoin, joined,
                participation == null ? null : mapper.participation(participation),
                payment == null ? null : mapper.payment(payment));
    }

    @Transactional
    public JoinEventResponse join(long eventId) {
        var member = currentMemberProvider.current();
        var event = findEvent(eventId);
        if (participationRepository.findByEventIdAndMemberId(eventId, member.getId()).isPresent()) {
            throw ApiException.conflict("ALREADY_PARTICIPATING", "이미 참가 신청한 행사입니다.");
        }
        Instant now = Instant.now(clock);
        event.validateJoin(now, joinedCount(eventId));
        var participation = participationRepository.save(new Participation(eventId, member.getId(), now));
        var payment = new PaymentObligation(member.getId(), PaymentType.EVENT_FEE, event.getFeeAmount(),
                PaymentSourceType.EVENT, eventId, event.getTitle(), event.getRegistrationDeadline(), now);
        payment.setDestination("카카오뱅크", "3333-12-3456789", "김총무", "https://qr.kakaopay.com/example");
        payment = paymentRepository.save(payment);
        historyRepository.save(new PaymentStatusHistory(payment.getId(), null, payment.getStatus(), null, now));
        return new JoinEventResponse(mapper.participation(participation), mapper.payment(payment));
    }

    @Transactional
    public CancelParticipationResponse cancel(long eventId, CancelParticipationRequest request) {
        var member = currentMemberProvider.current();
        var event = findEvent(eventId);
        var participation = participationRepository.findByEventIdAndMemberId(eventId, member.getId())
                .orElseThrow(() -> ApiException.notFound("참가 신청을 찾을 수 없습니다."));
        Instant now = Instant.now(clock);

        event.validateCancellation(now);
        participation.cancel(request.version(), now);
        participationRepository.saveAndFlush(participation);

        var payment = paymentRepository.findByMemberIdAndSourceTypeAndSourceId(
                member.getId(), PaymentSourceType.EVENT, eventId).orElse(null);
        PaymentStatus paymentStatus = PaymentStatus.NOT_REQUIRED;
        if (payment != null) {
            PaymentStatus previousStatus = payment.getStatus();
            payment.cancelForEvent(now);
            paymentRepository.saveAndFlush(payment);
            historyRepository.save(new PaymentStatusHistory(
                    payment.getId(), previousStatus, payment.getStatus(), request.reason(), now));
            paymentStatus = payment.getStatus();
        }

        return new CancelParticipationResponse(
                participation.getStatus(), paymentStatus, paymentStatus == PaymentStatus.REFUND_PENDING);
    }

    private EventListItem listItem(ClubEvent event, Long memberId) {
        var participation = participationRepository.findByEventIdAndMemberId(event.getId(), memberId).orElse(null);
        var payment = paymentRepository.findByMemberIdAndSourceTypeAndSourceId(memberId, PaymentSourceType.EVENT, event.getId()).orElse(null);
        return new EventListItem(event.getId(), event.getTitle(), event.getSummary(), event.getLocation(),
                event.getStartsAt(), event.getEndsAt(), event.getRegistrationDeadline(), event.getCapacity(),
                joinedCount(event.getId()), event.getFeeAmount(), event.getStatus(),
                participation == null ? null : participation.getStatus(), payment == null ? null : payment.getStatus());
    }

    private ClubEvent findEvent(long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> ApiException.notFound("행사를 찾을 수 없습니다."));
    }

    private long joinedCount(long eventId) {
        return participationRepository.countByEventIdAndStatus(eventId, ParticipationStatus.JOINED);
    }
}
