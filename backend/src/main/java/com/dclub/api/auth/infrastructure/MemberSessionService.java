package com.dclub.api.auth.infrastructure;

import com.dclub.api.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberSessionService {
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public void signIn(Member member, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            // CSRF 토큰 발급 과정에서 만들어진 세션 ID를 재사용하지 않도록 로그인 시 회전합니다.
            request.changeSessionId();
        }
        var principal = new MemberSessionPrincipal(member.getId());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
