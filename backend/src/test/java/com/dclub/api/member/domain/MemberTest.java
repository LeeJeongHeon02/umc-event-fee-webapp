package com.dclub.api.member.domain;

import com.dclub.api.global.common.ApiException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class MemberTest {
    private final Instant now = Instant.parse("2026-09-02T00:00:00Z");

    @Test
    void 온보딩을_마친_대기_회원은_승인할_수_있다() {
        Member member = Member.pendingKakaoMember("123", "길동", now);
        member.completeOnboarding("홍길동", MemberPart.PE_WEB, now.plusSeconds(1));

        member.approve(0, now.plusSeconds(2));

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.displayNickname()).isEqualTo("PE(Web) 홍길동");
    }

    @Test
    void 온보딩_전_회원은_승인할_수_없다() {
        Member member = Member.pendingKakaoMember("123", "길동", now);
        assertThatThrownBy(() -> member.approve(0, now.plusSeconds(1)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 부트스트랩_관리자는_온보딩과_함께_활성화된다() {
        Member member = Member.bootstrapAdmin("123", "관리자", now);
        member.completeOnboarding("김관리", MemberPart.PLAN, now.plusSeconds(1));
        assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void 로컬_회원은_인증정보를_보관하고_온보딩_대기로_시작한다() {
        Member member = Member.pendingLocalMember("local.member", "$2a$encoded", "01012345678", now);

        assertThat(member.getKakaoId()).isNull();
        assertThat(member.getLoginId()).isEqualTo("local.member");
        assertThat(member.getPasswordHash()).isEqualTo("$2a$encoded");
        assertThat(member.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(member.isOnboardingCompleted()).isFalse();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
    }
}
