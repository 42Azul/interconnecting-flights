package com.ryanair.interconnector.exception;

import com.ryanair.interconnectingflights.model.Error;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Optional;

/**
 * A global exception handler that simplifies controller code, provides consistent error responses, logs issues safely,
 * and improves maintainability and API caller communication.
 */

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // 5xx handlers
  @ExceptionHandler(ExternalApiException.class)
  public ResponseEntity<Error> handleApiException(ExternalApiException ex) {
    log.atError().setMessage(
            "Exception occurred with error code: {} and message: {}. Exception: {}")
        .addArgument(ex.getErrorType().getCode())
        .addArgument(ex.getMessage())
        .addArgument(ex)
        .log();

    return ResponseEntity
        .status(ex.getErrorType().getStatus())
        .body(new Error().code(ex.getErrorType().getCode())
            .message(ex.getErrorType().getMessage())
            .details(ex.getMessage()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Error> handleUnhandledRuntimeException(RuntimeException ex) {
    log.atError()
        .setMessage("Unhandled exception occurred: , {}")
        .addArgument(ex)
        .log();

    // We log it but we do not send it back as we do not want to leak internal issues
    Error apiError = new Error()
        .code(ErrorType.INTERNAL_ERROR.getCode())
        .message(ErrorType.INTERNAL_ERROR.getMessage())
        .details("Unexpected internal server error");

    return ResponseEntity
        .status(ErrorType.INTERNAL_ERROR.getStatus())
        .body(apiError);
  }

  // 4xx handlers. No need to log these as they are client errors but not the API server fault.

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<Error> handleMissingParams(MissingServletRequestParameterException ex) {

    String detail = "Missing required query parameter: '" + ex.getParameterName() + "'";

    Error apiError = new Error()
        .code(ErrorType.INVALID_REQUEST.getCode())
        .message(ErrorType.INVALID_REQUEST.getMessage())
        .details(detail);

    return ResponseEntity
        .status(ErrorType.INVALID_REQUEST.getStatus())
        .body(apiError);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Error> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String expectedType = Optional.ofNullable(ex.getRequiredType())
        .map(Class::getSimpleName)
        .orElse("unknown");

    String givenValue = Optional.ofNullable(ex.getValue())
        .map(Object::toString)
        .orElse("null");

    String paramName = Optional.of(ex.getName()).orElse("unknown");

    String detail = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
        givenValue, paramName, expectedType);

    return ResponseEntity
        .status(ErrorType.INVALID_REQUEST.getStatus())
        .body(new Error()
            .code(ErrorType.INVALID_REQUEST.getCode())
            .message(ErrorType.INVALID_REQUEST.getMessage())
            .details(detail));
  }

}
