package com.dclub.api.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Every failed API request uses this RFC 7807-compatible envelope.
 * The stable {@code code} is intended for client branching; {@code detail} is safe to show to users.
 */
@Schema(name = "Problem", description = "API 공통 오류 응답. Content-Type은 application/problem+json입니다.")
public record ProblemResponse(
        @Schema(description = "오류 유형 URI", example = "about:blank") String type,
        @Schema(description = "HTTP 상태 이름", example = "Bad Request") String title,
        @Schema(description = "HTTP 상태 코드", example = "400") int status,
        @Schema(description = "클라이언트가 분기 처리할 안정적인 오류 코드", example = "VALIDATION_FAILED") String code,
        @Schema(description = "사용자에게 표시할 수 있는 오류 설명", example = "요청 값을 확인해 주세요.") String detail,
        @Schema(description = "오류가 발생한 요청 경로", example = "/api/v1/me/onboarding") String instance,
        @Schema(description = "오류 발생 시각(UTC)") Instant timestamp,
        @Schema(description = "필드 단위 검증 오류. 검증 오류가 아니면 빈 배열입니다.") List<FieldErrorItem> fieldErrors) {

    @Schema(name = "FieldError", description = "요청 필드 하나의 검증 실패 내용")
    public record FieldErrorItem(
            @Schema(description = "오류 필드명", example = "name") String field,
            @Schema(description = "검증 실패 이유", example = "공백일 수 없습니다") String reason) {}
}
