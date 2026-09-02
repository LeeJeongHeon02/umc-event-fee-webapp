package com.dclub.api.event.domain;

import com.dclub.api.global.common.ApiException;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "participations", uniqueConstraints = @UniqueConstraint(name = "uk_participation_event_member", columnNames = {"event_id", "member_id"}))
public class Participation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ParticipationStatus status;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    @Column(name = "canceled_at")
    private Instant canceledAt;
    @Version
    private long version;

    protected Participation() {}

    public Participation(Long eventId, Long memberId, Instant joinedAt) {
        this.eventId = eventId;
        this.memberId = memberId;
        this.status = ParticipationStatus.JOINED;
        this.joinedAt = joinedAt;
    }

    public void cancel(long expectedVersion, Instant now) {
        if (version != expectedVersion) {
            throw ApiException.conflict("PARTICIPATION_STATE_CONFLICT", "참가 상태가 변경되었습니다. 새로고침 후 다시 시도해 주세요.");
        }
        if (status != ParticipationStatus.JOINED) {
            throw ApiException.conflict("PARTICIPATION_STATE_CONFLICT", "이미 취소된 참가 신청입니다.");
        }
        status = ParticipationStatus.CANCELED;
        canceledAt = now;
    }

    public Long getId() { return id; }
    public Long getEventId() { return eventId; }
    public Long getMemberId() { return memberId; }
    public ParticipationStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getCanceledAt() { return canceledAt; }
    public long getVersion() { return version; }
}
