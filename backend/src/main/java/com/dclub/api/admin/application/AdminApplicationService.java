package com.dclub.api.admin.application;

import com.dclub.api.global.presentation.ApiDtos.*;
import com.dclub.api.global.application.ApiMapper;
import com.dclub.api.global.common.ApiException;
import com.dclub.api.member.domain.*;
import com.dclub.api.event.domain.*;
import com.dclub.api.dues.domain.*;
import com.dclub.api.payment.domain.*;
import com.dclub.api.member.infrastructure.*;
import com.dclub.api.event.infrastructure.*;
import com.dclub.api.dues.infrastructure.*;
import com.dclub.api.payment.infrastructure.*;
import com.dclub.api.global.security.CurrentMemberProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class AdminApplicationService {
    private final MemberRepository memberRepository;
    private final ClubEventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final DuesRoundRepository duesRoundRepository;
    private final PaymentObligationRepository paymentRepository;
    private final PaymentReportRepository reportRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final CurrentMemberProvider currentMemberProvider;
    private final ApiMapper mapper;
    private final Clock clock;

    public AdminApplicationService(MemberRepository memberRepository, ClubEventRepository eventRepository,
                                   ParticipationRepository participationRepository,
                                   DuesRoundRepository duesRoundRepository,
                                   PaymentObligationRepository paymentRepository,
                                   PaymentReportRepository reportRepository,
                                   PaymentStatusHistoryRepository historyRepository,
                                   CurrentMemberProvider currentMemberProvider, ApiMapper mapper, Clock clock) {
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.duesRoundRepository = duesRoundRepository;
        this.paymentRepository = paymentRepository;
        this.reportRepository = reportRepository;
        this.historyRepository = historyRepository;
        this.currentMemberProvider = currentMemberProvider;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        currentMemberProvider.requireStaff();
        var events = eventRepository.findAllByStatusOrderByStartsAtAsc(EventStatus.PUBLISHED).stream()
                .map(this::eventSummary).toList();
        var duesRounds = duesRoundRepository.findAllByStatusOrderByDueAtAsc(DuesRoundStatus.PUBLISHED).stream()
                .map(this::duesSummary).toList();
        var allPayments = paymentRepository.findAll();
        long expected = allPayments.stream().mapToLong(PaymentObligation::getAmount).sum();
        long confirmed = allPayments.stream().filter(p -> p.getStatus() == PaymentStatus.CONFIRMED)
                .mapToLong(PaymentObligation::getAmount).sum();
        double rate = expected == 0 ? 100.0 : Math.round((confirmed * 10000.0 / expected)) / 100.0;
        var recent = allPayments.stream().filter(p -> p.getStatus() == PaymentStatus.REPORTED)
                .map(this::paymentRow)
                .sorted(Comparator.comparing((AdminPaymentRow row) -> row.latestReport() == null
                        ? Instant.EPOCH : row.latestReport().reportedAt()).reversed())
                .limit(10).toList();
        return new AdminDashboardResponse(
                memberRepository.countByStatus(MemberStatus.ACTIVE),
                memberRepository.countByStatus(MemberStatus.PENDING),
                events.size(),
                count(allPayments, PaymentStatus.UNPAID),
                count(allPayments, PaymentStatus.REPORTED),
                expected, confirmed, rate, events, duesRounds, recent);
    }

    @Transactional(readOnly = true)
    public AdminEventParticipantPage eventParticipants(long eventId) {
        currentMemberProvider.requireStaff();
        var event = eventRepository.findById(eventId).orElseThrow(() -> ApiException.notFound("행사를 찾을 수 없습니다."));
        var participations = participationRepository.findAllByEventIdAndStatusOrderByJoinedAtAsc(eventId, ParticipationStatus.JOINED);
        var items = participations.stream().map(participation -> {
            Member member = memberRepository.findById(participation.getMemberId())
                    .orElseThrow(() -> ApiException.notFound("부원을 찾을 수 없습니다."));
            var payment = paymentRepository.findByMemberIdAndSourceTypeAndSourceId(member.getId(), PaymentSourceType.EVENT, eventId).orElse(null);
            return new AdminEventParticipant(participation.getId(), member.getId(), member.displayNickname(),
                    member.getName(), member.getPart(), participation.getJoinedAt(),
                    payment == null ? null : payment.getId(), payment == null ? 0 : payment.getAmount(),
                    payment == null ? PaymentStatus.NOT_REQUIRED : payment.getStatus(),
                    payment == null ? null : latestReport(payment.getId()),
                    payment == null ? participation.getVersion() : payment.getVersion());
        }).toList();
        return new AdminEventParticipantPage(eventSummary(event), items, 0, Math.max(items.size(), 1), items.size(), items.isEmpty() ? 0 : 1);
    }

    @Transactional(readOnly = true)
    public AdminDuesPaymentPage duesPayments(long duesRoundId) {
        currentMemberProvider.requireStaff();
        var duesRound = duesRoundRepository.findById(duesRoundId)
                .orElseThrow(() -> ApiException.notFound("회비 차수를 찾을 수 없습니다."));
        var payments = paymentRepository.findAllBySourceTypeAndSourceIdOrderByMemberIdAsc(PaymentSourceType.DUES_ROUND, duesRoundId);
        var items = payments.stream().map(this::paymentRow).toList();
        return new AdminDuesPaymentPage(duesSummary(duesRound), items, 0, Math.max(items.size(), 1), items.size(), items.isEmpty() ? 0 : 1);
    }

    @Transactional
    public AdminPaymentReviewResponse review(long paymentId, AdminPaymentReviewRequest request, boolean confirm) {
        currentMemberProvider.requireStaff();
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("납부 항목을 찾을 수 없습니다."));
        if (payment.getVersion() != request.version()) {
            throw ApiException.conflict("PAYMENT_STATE_CONFLICT", "납부 상태가 변경되었습니다. 새로고침 후 다시 시도해 주세요.");
        }
        PaymentStatus previous = payment.getStatus();
        Instant now = Instant.now(clock);
        if (confirm) payment.confirm(now); else payment.reject(now);
        payment = paymentRepository.saveAndFlush(payment);
        historyRepository.save(new PaymentStatusHistory(payment.getId(), previous, payment.getStatus(), request.note(), now));
        return new AdminPaymentReviewResponse(payment.getId(), payment.getStatus(), payment.getVersion(), now);
    }

    private AdminEventSummary eventSummary(ClubEvent event) {
        var payments = paymentRepository.findAllBySourceTypeAndSourceIdOrderByMemberIdAsc(PaymentSourceType.EVENT, event.getId());
        return new AdminEventSummary(event.getId(), event.getTitle(), event.getStartsAt(), event.getCapacity(),
                participationRepository.countByEventIdAndStatus(event.getId(), ParticipationStatus.JOINED),
                event.getFeeAmount(), count(payments, PaymentStatus.UNPAID),
                count(payments, PaymentStatus.REPORTED), count(payments, PaymentStatus.CONFIRMED));
    }

    private AdminDuesRoundSummary duesSummary(DuesRound duesRound) {
        var payments = paymentRepository.findAllBySourceTypeAndSourceIdOrderByMemberIdAsc(PaymentSourceType.DUES_ROUND, duesRound.getId());
        long confirmedCount = count(payments, PaymentStatus.CONFIRMED);
        return new AdminDuesRoundSummary(duesRound.getId(), duesRound.getTitle(), duesRound.getAmount(),
                duesRound.getDueAt(), payments.size(), count(payments, PaymentStatus.UNPAID),
                count(payments, PaymentStatus.REPORTED), confirmedCount, confirmedCount * duesRound.getAmount());
    }

    private AdminPaymentRow paymentRow(PaymentObligation payment) {
        Member member = memberRepository.findById(payment.getMemberId())
                .orElseThrow(() -> ApiException.notFound("부원을 찾을 수 없습니다."));
        return new AdminPaymentRow(payment.getId(), member.getId(), member.displayNickname(), member.getName(),
                member.getPart(), payment.getAmount(), payment.getStatus(), payment.getDueAt(),
                latestReport(payment.getId()), payment.getVersion());
    }

    private PaymentReportResponseItem latestReport(Long paymentId) {
        return reportRepository.findFirstByPaymentObligationIdOrderByReportedAtDesc(paymentId)
                .map(mapper::report).orElse(null);
    }

    private long count(List<PaymentObligation> payments, PaymentStatus status) {
        return payments.stream().filter(payment -> payment.getStatus() == status).count();
    }
}
