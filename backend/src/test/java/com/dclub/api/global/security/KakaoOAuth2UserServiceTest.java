package com.dclub.api.global.security;

import com.dclub.api.member.domain.Member;
import com.dclub.api.member.infrastructure.MemberRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KakaoOAuth2UserServiceTest {

    @Test
    void 최초_카카오_로그인이면_온보딩_대기_회원을_생성한다() {
        var repository = mock(MemberRepository.class);
        var now = Instant.parse("2026-09-02T00:00:00Z");
        var service = new KakaoOAuth2UserService(repository, Clock.fixed(now, ZoneOffset.UTC), "");
        when(repository.findByKakaoId("12345")).thenReturn(Optional.empty());
        when(repository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member member = service.findOrCreateMember("12345", " 카카오닉네임 ");

        assertThat(member.getKakaoId()).isEqualTo("12345");
        assertThat(member.getKakaoProfileName()).isEqualTo("카카오닉네임");
        assertThat(member.isOnboardingCompleted()).isFalse();
        verify(repository).save(any(Member.class));
    }

    @Test
    void 재로그인이면_기존_회원을_재사용한다() {
        var repository = mock(MemberRepository.class);
        var existing = Member.pendingKakaoMember("12345", "기존닉네임", Instant.EPOCH);
        when(repository.findByKakaoId("12345")).thenReturn(Optional.of(existing));
        var service = new KakaoOAuth2UserService(repository, Clock.systemUTC(), "");

        assertThat(service.findOrCreateMember("12345", "변경닉네임")).isSameAs(existing);
        verify(repository, never()).save(any());
    }
}
