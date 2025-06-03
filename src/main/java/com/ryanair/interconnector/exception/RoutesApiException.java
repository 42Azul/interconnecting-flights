package com.ryanair.interconnector.exception;

public class RoutesApiException extends ApiException {
    public RoutesApiException(String message, Throwable cause) {
        super(message, cause, ErrorType.EXTERNAL_API_ERROR);
    }
}