package com.dclub.api.global.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Creates the same error envelope for MVC controllers and the Spring Security filter chain. */
@Component
public class ProblemResponseFactory {
    private final Clock clock;

    public ProblemResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public ProblemResponse create(HttpStatus status, String code, String detail,
                                  HttpServletRequest request,
                                  List<ProblemResponse.FieldErrorItem> fieldErrors) {
        return new ProblemResponse(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                code,
                detail,
                request.getRequestURI(),
                Instant.now(clock),
                List.copyOf(fieldErrors));
    }

    public ResponseEntity<ProblemResponse> response(HttpStatus status, String code, String detail,
                                                    HttpServletRequest request,
                                                    List<ProblemResponse.FieldErrorItem> fieldErrors) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(create(status, code, detail, request, fieldErrors));
    }
}
