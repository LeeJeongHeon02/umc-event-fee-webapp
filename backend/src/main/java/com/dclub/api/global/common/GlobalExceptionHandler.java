package com.dclub.api.global.common;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/** Maps every controller-layer failure to the shared, documented Problem response. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemResponseFactory problemFactory;

    public GlobalExceptionHandler(ProblemResponseFactory problemFactory) {
        this.problemFactory = problemFactory;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemResponse> handleApi(ApiException exception, HttpServletRequest request) {
        return problemFactory.response(exception.status(), exception.code(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> handleValidation(MethodArgumentNotValidException exception,
                                                      HttpServletRequest request) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ProblemResponse.FieldErrorItem(error.getField(),
                        error.getDefaultMessage() == null ? "올바르지 않은 값입니다." : error.getDefaultMessage()))
                .toList();
        return problemFactory.response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "요청 값을 확인해 주세요.", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemResponse> handleConstraintViolation(ConstraintViolationException exception,
                                                              HttpServletRequest request) {
        var fields = exception.getConstraintViolations().stream()
                .map(violation -> new ProblemResponse.FieldErrorItem(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return problemFactory.response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "요청 값을 확인해 주세요.", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ProblemResponse> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        String code = exception instanceof HttpMessageNotReadableException ? "MALFORMED_JSON" : "INVALID_REQUEST";
        return problemFactory.response(HttpStatus.BAD_REQUEST, code,
                "요청 형식이나 파라미터 값을 확인해 주세요.", request, List.of());
    }

    @ExceptionHandler({DataIntegrityViolationException.class,
            OptimisticLockException.class,
            ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ProblemResponse> handleConflict(Exception exception, HttpServletRequest request) {
        return problemFactory.response(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "다른 요청으로 상태가 변경되었습니다. 새로고침 후 다시 시도해 주세요.", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemResponse> handleIllegalArgument(IllegalArgumentException exception,
                                                          HttpServletRequest request) {
        return problemFactory.response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemResponse> handleNoResource(NoResourceFoundException exception,
                                                     HttpServletRequest request) {
        return problemFactory.response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "요청한 API를 찾을 수 없습니다.", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception,
                                                           HttpServletRequest request) {
        return problemFactory.response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "지원하지 않는 HTTP 메서드입니다.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        // Log the original exception only on the server; never leak stack traces or DB details to clients.
        log.error("Unhandled API error: {} {}", request.getMethod(), request.getRequestURI(), exception);
        return problemFactory.response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", request, List.of());
    }
}
