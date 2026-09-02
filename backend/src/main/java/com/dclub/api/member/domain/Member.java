package com.dclub.api.member.domain;

import jakarta.persistence.*;
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

    public void completeOnboarding(String name, MemberPart part, Instant now) {
        this.name = name;
        this.part = part;
        this.onboardingCompleted = true;
        if (this.status != MemberStatus.ACTIVE) this.status = MemberStatus.PENDING;
        this.updatedAt = now;
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
    public long getVersion() { return version; }
}
