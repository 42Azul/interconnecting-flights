package com.ryanair.interconnector.exception;

import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {

    private final ErrorType errorType = ErrorType.EXTERNAL_API_ERROR;

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
