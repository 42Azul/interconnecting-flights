package com.ryanair.interconnector.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
class DummyController {

  @GetMapping("/schedule-exception/{origin}/{destination}/{year}/{month}")
  public String throwScheduleException(
      @PathVariable String origin,
      @PathVariable String destination,
      @PathVariable Integer year,
      @PathVariable Integer month) {

    String message = "Failed to fetch schedule from %s to %s (%d-%02d)"
        .formatted(origin, destination, year, month);

    throw new ExternalApiException(message, new RuntimeException("simulated failure"));
  }

  @GetMapping("/routes-exception")
  public String throwRoutesApiException() {
    throw new ExternalApiException("Error in Routes API", new RuntimeException("cause"));
  }

  @GetMapping("/runtime-exception")
  public String throwRuntimeException() {
    throw new IllegalStateException("boom");
  }

  @GetMapping("/required-param")
  public String missingParam(@RequestParam(required = true, name = "intParam") int intParam) {
    return "OK";
  }

}
