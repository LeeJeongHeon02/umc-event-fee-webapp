package com.dclub.api.member.domain;

import jakarta.persistence.*;
import com.dclub.api.global.common.ApiException;
import java.time.Instant;

@Entity
@Table(name = "members")
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "kakao_id", nullable = false, unique = true)
    private String kakaoId;
    @Column(name = "kakao_profile_name", nullable = false)
    private String kakaoProfileName;
    private String name;
    @Enumerated(EnumType.STRING)
    private MemberPart part;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MemberRole role;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MemberStatus status;
    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected Member() {}

    private Member(String kakaoId, String kakaoProfileName, String name, MemberPart part,
                   MemberRole role, MemberStatus status, boolean onboardingCompleted, Instant now) {
        this.kakaoId = kakaoId;
        this.kakaoProfileName = kakaoProfileName;
        this.name = name;
        this.part = part;
        this.role = role;
        this.status = status;
        this.onboardingCompleted = onboardingCompleted;
        this.approvedAt = status == MemberStatus.ACTIVE ? now : null;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Member activeStaff(String kakaoId, String profileName, String name, MemberPart part, Instant now) {
        return new Member(kakaoId, profileName, name, part, MemberRole.STAFF, MemberStatus.ACTIVE, true, now);
    }

    public static Member activeMember(String kakaoId, String profileName, String name, MemberPart part, Instant now) {
        return new Member(kakaoId, profileName, name, part, MemberRole.MEMBER, MemberStatus.ACTIVE, true, now);
    }

    public static Member pendingKakaoMember(String kakaoId, String profileName, Instant now) {
        return new Member(kakaoId, profileName, null, null, MemberRole.MEMBER, MemberStatus.PENDING, false, now);
    }

    public static Member bootstrapAdmin(String kakaoId, String profileName, Instant now) {
        return new Member(kakaoId, profileName, null, null, MemberRole.ADMIN, MemberStatus.PENDING, false, now);
    }

    public void completeOnboarding(String name, MemberPart part, Instant now) {
        this.name = name;
        this.part = part;
        this.onboardingCompleted = true;
        if (role == MemberRole.ADMIN) {
            this.status = MemberStatus.ACTIVE;
            this.approvedAt = now;
        } else if (this.status != MemberStatus.ACTIVE) {
            this.status = MemberStatus.PENDING;
        }
        this.updatedAt = now;
    }

    public void approve(long expectedVersion, Instant now) {
        validateVersion(expectedVersion);
        if (!onboardingCompleted || status != MemberStatus.PENDING) {
            throw ApiException.conflict("MEMBER_STATE_CONFLICT", "승인 대기 중인 회원만 승인할 수 있습니다.");
        }
        status = MemberStatus.ACTIVE;
        approvedAt = now;
        updatedAt = now;
    }

    public void suspend(long expectedVersion, Instant now) {
        validateVersion(expectedVersion);
        if (status != MemberStatus.ACTIVE) {
            throw ApiException.conflict("MEMBER_STATE_CONFLICT", "활동 중인 회원만 정지할 수 있습니다.");
        }
        status = MemberStatus.SUSPENDED;
        updatedAt = now;
    }

    public void changeRole(MemberRole nextRole, long expectedVersion, Instant now) {
        validateVersion(expectedVersion);
        if (status != MemberStatus.ACTIVE) {
            throw ApiException.conflict("MEMBER_STATE_CONFLICT", "활동 중인 회원의 역할만 변경할 수 있습니다.");
        }
        role = nextRole;
        updatedAt = now;
    }

    public void promoteBootstrapAdmin(Instant now) {
        role = MemberRole.ADMIN;
        if (onboardingCompleted) {
            status = MemberStatus.ACTIVE;
            approvedAt = now;
        }
        updatedAt = now;
    }

    private void validateVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw ApiException.conflict("MEMBER_STATE_CONFLICT", "회원 정보가 변경되었습니다. 새로고침해 주세요.");
        }
    }

    public boolean canManage() {
        return role == MemberRole.STAFF || role == MemberRole.ADMIN;
    }

    public String displayNickname() {
        if (name == null || part == null) return null;
        return switch (part) {
            case PLAN -> "Plan " + name;
            case DESIGN -> "Design " + name;
            case PE_WEB -> "PE(Web) " + name;
            case PE_MOBILE -> "PE(Mobile) " + name;
        };
    }

    public Long getId() { return id; }
    public String getKakaoId() { return kakaoId; }
    public String getKakaoProfileName() { return kakaoProfileName; }
    public String getName() { return name; }
    public MemberPart getPart() { return part; }
    public MemberRole getRole() { return role; }
    public MemberStatus getStatus() { return status; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
