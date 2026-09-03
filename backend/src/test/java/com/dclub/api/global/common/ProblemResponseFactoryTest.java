package com.dclub.api.global.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemResponseFactoryTest {

    @Test
    void 요청_경로와_UTC_시각을_포함한_공통_오류를_만든다() {
        Instant occurredAt = Instant.parse("2026-09-03T09:30:00Z");
        var factory = new ProblemResponseFactory(Clock.fixed(occurredAt, ZoneOffset.UTC));
        var request = new MockHttpServletRequest("PATCH", "/api/v1/me/onboarding");
        var fieldErrors = List.of(new ProblemResponse.FieldErrorItem("name", "공백일 수 없습니다"));

        ProblemResponse problem = factory.create(
                HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값을 확인해 주세요.", request, fieldErrors);

        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.title()).isEqualTo("Bad Request");
        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(problem.instance()).isEqualTo("/api/v1/me/onboarding");
        assertThat(problem.timestamp()).isEqualTo(occurredAt);
        assertThat(problem.fieldErrors()).containsExactlyElementsOf(fieldErrors);
    }
}
