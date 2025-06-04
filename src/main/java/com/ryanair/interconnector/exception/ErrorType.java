package com.ryanair.interconnector.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enum representing different error types with associated HTTP status codes, error codes, and messages.
 * Used to standardize error handling across the application.
 */
@Getter
@AllArgsConstructor
public enum ErrorType {
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5001, "An internal error occurred"),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, 5002, "An error occurred while calling an external API"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 4000, "The request is invalid"),
    NOT_FOUND(HttpStatus.NOT_FOUND, 4001, "Resource not found");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
