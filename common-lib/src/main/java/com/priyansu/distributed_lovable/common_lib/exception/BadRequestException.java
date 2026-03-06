package com.priyansu.distributed_lovable.common_lib.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class BadRequestException extends RuntimeException {
    private final String message;
}
