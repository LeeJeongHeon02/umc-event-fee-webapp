package com.dclub.api.global.security;

import com.dclub.api.global.common.ProblemResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ApiSecurityExceptionHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private ApiSecurityExceptionHandler handler;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-03T09:30:00Z"), ZoneOffset.UTC);
        handler = new ApiSecurityExceptionHandler(new ProblemResponseFactory(clock), objectMapper);
    }

    @Test
    void 인증되지_않은_API_요청은_공통_401_응답을_반환한다() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/me");
        var response = new MockHttpServletResponse();

        handler.commence(request, response, new InsufficientAuthenticationException("unauthenticated"));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(body.get("code").asText()).isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(body.get("instance").asText()).isEqualTo("/api/v1/me");
    }

    @Test
    void 권한과_CSRF_오류를_구분해_공통_403_응답을_반환한다() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/admin/events");
        var forbiddenResponse = new MockHttpServletResponse();
        var csrfResponse = new MockHttpServletResponse();

        handler.handle(request, forbiddenResponse, new AccessDeniedException("forbidden"));
        handler.handle(request, csrfResponse, new MissingCsrfTokenException(null));

        assertThat(objectMapper.readTree(forbiddenResponse.getContentAsByteArray()).get("code").asText())
                .isEqualTo("FORBIDDEN");
        assertThat(objectMapper.readTree(csrfResponse.getContentAsByteArray()).get("code").asText())
                .isEqualTo("CSRF_TOKEN_INVALID");
    }
}
