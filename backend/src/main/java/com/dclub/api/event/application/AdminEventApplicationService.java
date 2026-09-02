package com.dclub.api.event.application;

import com.dclub.api.global.common.ApiException;
import com.dclub.api.event.domain.ClubEvent;
import com.dclub.api.event.domain.EventStatus;
import com.dclub.api.event.domain.ParticipationStatus;
import com.dclub.api.event.infrastructure.ClubEventRepository;
import com.dclub.api.event.infrastructure.ParticipationRepository;
import com.dclub.api.global.security.CurrentMemberProvider;
import com.dclub.api.global.presentation.ApiDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class AdminEventApplicationService {
    private final ClubEventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final CurrentMemberProvider currentMemberProvider;
    private final Clock clock;

    public AdminEventApplicationService(ClubEventRepository eventRepository,
                                        ParticipationRepository participationRepository,
                                        CurrentMemberProvider currentMemberProvider,
                                        Clock clock) {
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.currentMemberProvider = currentMemberProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminEventResponse> getAll() {
        currentMemberProvider.requireStaff();
        return eventRepository.findAllByOrderByStartsAtDesc().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public AdminEventResponse get(long eventId) {
        currentMemberProvider.requireStaff();
        return response(findEvent(eventId));
    }

    @Transactional
    public AdminEventResponse create(AdminEventCreateRequest request) {
        currentMemberProvider.requireStaff();
        Instant now = Instant.now(clock);
        var event = new ClubEvent(request.title().trim(), trimNullable(request.summary()),
                request.description().trim(), trimNullable(request.location()), request.startsAt(), request.endsAt(),
                request.registrationDeadline(), request.capacity(), request.feeAmount(), EventStatus.DRAFT,
                request.allowLateCancellation(), now);
        return response(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public AdminEventResponse update(long eventId, AdminEventUpdateRequest request) {
        currentMemberProvider.requireStaff();
        ClubEvent event = findEvent(eventId);
        event.updateDraft(request.title().trim(), trimNullable(request.summary()), request.description().trim(),
                trimNullable(request.location()), request.startsAt(), request.endsAt(), request.registrationDeadline(),
                request.capacity(), request.feeAmount(), request.allowLateCancellation(), request.version(),
                Instant.now(clock));
        return response(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public AdminEventResponse publish(long eventId, AdminEventVersionRequest request) {
        currentMemberProvider.requireStaff();
        ClubEvent event = findEvent(eventId);
        event.publish(request.version(), Instant.now(clock));
        return response(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public void delete(long eventId, long version) {
        currentMemberProvider.requireStaff();
        ClubEvent event = findEvent(eventId);
        event.validateDeletable(version, participationRepository.countByEventId(eventId));
        eventRepository.delete(event);
    }

    private ClubEvent findEvent(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("행사를 찾을 수 없습니다."));
    }

    private AdminEventResponse response(ClubEvent event) {
        return new AdminEventResponse(event.getId(), event.getTitle(), event.getSummary(), event.getDescription(),
                event.getLocation(), event.getStartsAt(), event.getEndsAt(), event.getRegistrationDeadline(),
                event.getCapacity(), participationRepository.countByEventIdAndStatus(event.getId(), ParticipationStatus.JOINED),
                event.getFeeAmount(), event.getStatus(), event.isAllowLateCancellation(), event.getCreatedAt(),
                event.getUpdatedAt(), event.getVersion());
    }

    private String trimNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
