package com.priyansu.distributed_lovable.common_lib.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        HttpStatus status,
        String message,
        Instant timeStamp,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ApiFieldError> fieldErrors
) {
    //constructor
    public ApiError(HttpStatus status, String message) {
        this(status, message, Instant.now(), null);
    }

    public ApiError(HttpStatus status, String message, List<ApiFieldError> fieldErrors) {
        this(status, message, Instant.now(), fieldErrors);
    }
}

//just for validation error
record ApiFieldError(
        String field,
        String message
) {}
