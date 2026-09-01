package com.dclub.api.common;

import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemResponse> handleApi(ApiException exception) {
        return problem(exception.status(), exception.code(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> handleValidation(MethodArgumentNotValidException exception) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ProblemResponse.FieldErrorItem(error.getField(), error.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값을 확인해 주세요.", fields);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockException.class})
    ResponseEntity<ProblemResponse> handleConflict(Exception exception) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "다른 요청으로 상태가 변경되었습니다. 새로고침 후 다시 시도해 주세요.", List.of());
    }

    private ResponseEntity<ProblemResponse> problem(HttpStatus status, String code, String detail,
                                                    List<ProblemResponse.FieldErrorItem> fields) {
        var body = new ProblemResponse(status.getReasonPhrase(), status.value(), code, detail, Instant.now(), fields);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
