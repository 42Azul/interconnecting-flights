package com.ryanair.interconnector.client;

import com.ryanair.interconnector.exception.ExternalApiException;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

/**
 * Custom error decoder for handling errors from external APIs
 */
@Component
public class ExternalApiErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    String api = methodKey.contains("Schedules") ? "Schedules" : "Routes";
    int status = response.status();
    String reason = response.reason();

    String message = "[%s API] call failed (%d %s)".formatted(api, status, reason);

    return new ExternalApiException(message, FeignException.errorStatus(methodKey, response));
  }
}
