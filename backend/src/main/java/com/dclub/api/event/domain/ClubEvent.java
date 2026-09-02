package com.dclub.api.event.domain;

import com.dclub.api.global.common.ApiException;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "club_events")
public class ClubEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    private String summary;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    private String location;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "ends_at")
    private Instant endsAt;
    @Column(name = "registration_deadline", nullable = false)
    private Instant registrationDeadline;
    private Integer capacity;
    @Column(name = "fee_amount", nullable = false)
    private long feeAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EventStatus status;
    @Column(name = "allow_late_cancellation", nullable = false)
    private boolean allowLateCancellation;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected ClubEvent() {}

    public ClubEvent(String title, String summary, String description, String location, Instant startsAt,
                     Instant endsAt, Instant registrationDeadline, Integer capacity, long feeAmount,
                     EventStatus status, boolean allowLateCancellation, Instant now) {
        validateFields(title, description, startsAt, endsAt, registrationDeadline, capacity, feeAmount);
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.location = location;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.registrationDeadline = registrationDeadline;
        this.capacity = capacity;
        this.feeAmount = feeAmount;
        this.status = status;
        this.allowLateCancellation = allowLateCancellation;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDraft(String title, String summary, String description, String location, Instant startsAt,
                            Instant endsAt, Instant registrationDeadline, Integer capacity, long feeAmount,
                            boolean allowLateCancellation, long expectedVersion, Instant now) {
        validateExpectedVersion(expectedVersion);
        if (status != EventStatus.DRAFT) {
            throw ApiException.conflict("EVENT_NOT_EDITABLE", "초안 상태의 행사만 수정할 수 있습니다.");
        }
        validateFields(title, description, startsAt, endsAt, registrationDeadline, capacity, feeAmount);
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.location = location;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.registrationDeadline = registrationDeadline;
        this.capacity = capacity;
        this.feeAmount = feeAmount;
        this.allowLateCancellation = allowLateCancellation;
        this.updatedAt = now;
    }

    public void publish(long expectedVersion, Instant now) {
        validateExpectedVersion(expectedVersion);
        if (status != EventStatus.DRAFT) {
            throw ApiException.conflict("EVENT_NOT_DRAFT", "초안 상태의 행사만 공개할 수 있습니다.");
        }
        status = EventStatus.PUBLISHED;
        updatedAt = now;
    }

    public void validateDeletable(long expectedVersion, long participationCount) {
        validateExpectedVersion(expectedVersion);
        if (status != EventStatus.DRAFT || participationCount > 0) {
            throw ApiException.conflict("EVENT_NOT_DELETABLE", "참가 이력이 없는 초안 행사만 삭제할 수 있습니다.");
        }
    }

    private void validateExpectedVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw ApiException.conflict("EVENT_STATE_CONFLICT", "행사 정보가 변경되었습니다. 새로고침 후 다시 시도해 주세요.");
        }
    }

    private static void validateFields(String title, String description, Instant startsAt, Instant endsAt,
                                       Instant registrationDeadline, Integer capacity, long feeAmount) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("행사 제목은 필수입니다.");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("행사 설명은 필수입니다.");
        if (startsAt == null || registrationDeadline == null) throw new IllegalArgumentException("행사 일시와 신청 마감은 필수입니다.");
        if (registrationDeadline.isAfter(startsAt)) throw new IllegalArgumentException("신청 마감은 행사 시작 전이어야 합니다.");
        if (endsAt != null && endsAt.isBefore(startsAt)) throw new IllegalArgumentException("행사 종료는 시작 이후여야 합니다.");
        if (feeAmount < 0) throw new IllegalArgumentException("참가비는 음수일 수 없습니다.");
        if (capacity != null && capacity <= 0) throw new IllegalArgumentException("정원은 양수여야 합니다.");
    }

    public void validateJoin(Instant now, long joinedCount) {
        if (status != EventStatus.PUBLISHED) throw ApiException.conflict("EVENT_NOT_OPEN", "참가 신청 가능한 행사가 아닙니다.");
        if (now.isAfter(registrationDeadline)) throw ApiException.conflict("REGISTRATION_CLOSED", "참가 신청 기한이 종료되었습니다.");
        if (capacity != null && joinedCount >= capacity) throw ApiException.conflict("EVENT_CAPACITY_EXCEEDED", "행사 정원이 가득 찼습니다.");
    }

    public void validateCancellation(Instant now) {
        if (now.isAfter(registrationDeadline) && !allowLateCancellation) {
            throw ApiException.conflict("CANCELLATION_CLOSED", "참가 취소 기한이 종료되었습니다.");
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getRegistrationDeadline() { return registrationDeadline; }
    public Integer getCapacity() { return capacity; }
    public long getFeeAmount() { return feeAmount; }
    public EventStatus getStatus() { return status; }
    public boolean isAllowLateCancellation() { return allowLateCancellation; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
