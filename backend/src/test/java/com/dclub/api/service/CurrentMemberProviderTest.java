package com.dclub.api.service;

import com.dclub.api.common.ApiException;
import com.dclub.api.domain.Member;
import com.dclub.api.domain.MemberPart;
import com.dclub.api.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentMemberProviderTest {

    @Test
    void 운영_환경에서는_개발용_회원_인증을_거부한다() {
        var repository = mock(MemberRepository.class);
        var request = mock(HttpServletRequest.class);
        var provider = new CurrentMemberProvider(repository, 1, false, false, request);

        assertThatThrownBy(provider::current)
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).code())
                        .isEqualTo("AUTHENTICATION_REQUIRED"));
        verifyNoInteractions(repository, request);
    }

    @Test
    void 개발_환경에서는_헤더로_예시_회원을_전환할_수_있다() {
        var repository = mock(MemberRepository.class);
        var request = mock(HttpServletRequest.class);
        var member = Member.activeMember("dev-2", "민지", "김민지", MemberPart.DESIGN, Instant.EPOCH);
        when(request.getHeader("X-Dev-Member-Id")).thenReturn("2");
        when(repository.findById(2L)).thenReturn(Optional.of(member));
        var provider = new CurrentMemberProvider(repository, 1, true, true, request);

        assertThat(provider.current()).isSameAs(member);
    }

    @Test
    void 잘못된_개발용_회원_헤더는_400_오류로_거부한다() {
        var repository = mock(MemberRepository.class);
        var request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Dev-Member-Id")).thenReturn("member-two");
        var provider = new CurrentMemberProvider(repository, 1, true, true, request);

        assertThatThrownBy(provider::current)
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).code())
                        .isEqualTo("INVALID_DEV_MEMBER"));
        verifyNoInteractions(repository);
    }
}
