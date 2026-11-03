package com.houkiang.ordersettlement.web;

import java.util.List;

public record ApiError(String code, String message, List<FieldViolation> errors) {

    public ApiError {
        errors = List.copyOf(errors);
    }

    public record FieldViolation(String field, String message) {
    }
}
