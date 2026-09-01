package com.dclub.api.domain;

import com.dclub.api.common.ApiException;
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
        if (feeAmount < 0) throw new IllegalArgumentException("참가비는 음수일 수 없습니다.");
        if (capacity != null && capacity <= 0) throw new IllegalArgumentException("정원은 양수여야 합니다.");
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
    public long getVersion() { return version; }
}
