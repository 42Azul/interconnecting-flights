package com.ryanair.interconnector.exception;

import com.ryanair.interconnectingflights.model.Error;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public abstract class ApiException extends RuntimeException {

  private final ErrorType errorType;

  public ApiException(String message, Throwable cause, ErrorType errorType) {
    super(message, cause);
    this.errorType = errorType;
  }

  public Error getApiError() {
    return new Error()
        .code(errorType.getCode())
        .message(errorType.getMessage())
        .details(this.getMessage());
  }

  public HttpStatus getStatus() {
    return errorType.getStatus();
  }
}
