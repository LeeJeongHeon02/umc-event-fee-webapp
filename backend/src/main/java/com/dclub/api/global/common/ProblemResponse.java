package com.dclub.api.global.common;

import java.time.Instant;
import java.util.List;

public record ProblemResponse(String title, int status, String code, String detail,
                              Instant timestamp, List<FieldErrorItem> fieldErrors) {
    public record FieldErrorItem(String field, String reason) {}
}
