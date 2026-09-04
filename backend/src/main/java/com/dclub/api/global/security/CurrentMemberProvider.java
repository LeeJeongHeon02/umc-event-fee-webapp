package com.dclub.api.global.security;

import com.dclub.api.auth.infrastructure.MemberSessionPrincipal;
import com.dclub.api.global.common.ApiException;
import com.dclub.api.member.domain.Member;
import com.dclub.api.member.domain.MemberRole;
import com.dclub.api.member.domain.MemberStatus;
import com.dclub.api.member.infrastructure.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CurrentMemberProvider {
    private final MemberRepository memberRepository;
    private final long developmentMemberId;
    private final boolean developmentAuthenticationEnabled;
    private final boolean allowDevelopmentMemberHeader;
    private final HttpServletRequest request;

    public CurrentMemberProvider(MemberRepository memberRepository,
                                 @Value("${app.auth.dev-member-id:1}") long developmentMemberId,
                                 @Value("${app.auth.development-mode-enabled:false}") boolean developmentAuthenticationEnabled,
                                 @Value("${app.auth.allow-dev-member-header:false}") boolean allowDevelopmentMemberHeader,
                                 HttpServletRequest request) {
        this.memberRepository = memberRepository;
        this.developmentMemberId = developmentMemberId;
        this.developmentAuthenticationEnabled = developmentAuthenticationEnabled;
        this.allowDevelopmentMemberHeader = allowDevelopmentMemberHeader;
        this.request = request;
    }

    public Member current() {
        Long authenticatedMemberId = authenticatedMemberId();
        if (authenticatedMemberId != null) {
            return memberRepository.findById(authenticatedMemberId)
                    .orElseThrow(() -> ApiException.notFound("로그인 회원을 찾을 수 없습니다."));
        }
        if (developmentAuthenticationEnabled) {
            return memberRepository.findById(resolveDevelopmentMemberId())
                    .orElseThrow(() -> ApiException.notFound("개발용 로그인 회원을 찾을 수 없습니다."));
        }

        throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
    }

    private Long authenticatedMemberId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MemberSessionPrincipal principal) {
            return principal.memberId();
        }
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauthUser) {
            Object memberId = oauthUser.getAttribute("memberId");
            if (memberId instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }

    private long resolveDevelopmentMemberId() {
        if (!allowDevelopmentMemberHeader) return developmentMemberId;
        String header = request.getHeader("X-Dev-Member-Id");
        if (header == null || header.isBlank()) return developmentMemberId;
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEV_MEMBER", "개발용 회원 ID가 올바르지 않습니다.");
        }
    }

    public Member requireStaff() {
        Member member = requireActive();
        if (!member.canManage()) throw ApiException.forbidden("운영진 권한이 필요합니다.");
        return member;
    }

    public Member requireActive() {
        Member member = current();
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw ApiException.forbidden("승인된 활동 회원만 이용할 수 있습니다.");
        }
        return member;
    }

    public Member requireAdmin() {
        Member member = requireActive();
        if (member.getRole() != MemberRole.ADMIN) throw ApiException.forbidden("최고 관리자 권한이 필요합니다.");
        return member;
    }
}
