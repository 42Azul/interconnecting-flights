package com.ryanair.interconnector.exception;

public class ScheduleApiException extends ApiException {
    public ScheduleApiException(String from, String to, int year, int month, Throwable cause) {
        super(
            "Failed to fetch schedule from %s to %s (%d-%02d)".formatted(from, to, year, month),
            cause,
            ErrorType.EXTERNAL_API_ERROR
        );
    }
}
