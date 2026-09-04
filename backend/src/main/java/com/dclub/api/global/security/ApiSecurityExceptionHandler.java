package com.dclub.api.global.security;

import com.dclub.api.global.common.ProblemResponse;
import com.dclub.api.global.common.ProblemResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Converts failures raised before a controller is invoked into the same JSON error contract used by MVC.
 * Without this bridge, authentication and CSRF failures would return redirects or container-specific bodies.
 */
@Component
public class ApiSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ProblemResponseFactory problemFactory;
    private final ObjectMapper objectMapper;

    public ApiSecurityExceptionHandler(ProblemResponseFactory problemFactory, ObjectMapper objectMapper) {
        this.problemFactory = problemFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(response, problemFactory.create(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "로그인이 필요하거나 로그인 세션이 만료되었습니다.",
                request,
                List.of()));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException exception) throws IOException {
        boolean csrfFailure = exception instanceof CsrfException;
        write(response, problemFactory.create(
                HttpStatus.FORBIDDEN,
                csrfFailure ? "CSRF_TOKEN_INVALID" : "FORBIDDEN",
                csrfFailure
                        ? "보안 토큰이 없거나 만료되었습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요."
                        : "요청을 수행할 권한이 없습니다.",
                request,
                List.of()));
    }

    private void write(HttpServletResponse response, ProblemResponse body) throws IOException {
        response.setStatus(body.status());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
