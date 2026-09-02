package com.dclub.api.global.security;

import com.dclub.api.member.infrastructure.MemberRepository;
import com.dclub.api.member.domain.MemberStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final MemberRepository memberRepository;
    private final String frontendBaseUrl;

    public OAuthLoginSuccessHandler(MemberRepository memberRepository,
                                    @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.memberRepository = memberRepository;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Number memberId = principal.getAttribute("memberId");
        var member = memberRepository.findById(memberId.longValue()).orElseThrow();
        String route = !member.isOnboardingCompleted() ? "/onboarding"
                : member.getStatus() == MemberStatus.ACTIVE ? "/home" : "/pending";
        response.sendRedirect(frontendBaseUrl + route);
    }
}
