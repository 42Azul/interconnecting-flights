package com.ryanair.interconnector.validation;

import com.ryanair.interconnector.dto.FlightSlot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Duration;

/**
 * Encapsulates the logic to validate flight connections based on minimum layover time.
 */
@Component
public class MinimumLayoverValidator implements  FlightConnectionValidator {

  private final Duration minConnectionTime;

  public MinimumLayoverValidator(
      @Value("${interconnector.min-layover:PT2H}") Duration minConnectionTime
  ) {
    this.minConnectionTime = minConnectionTime;
  }

  public boolean isValidConnection(FlightSlot firstSlot, FlightSlot secondSlot) {
    if (firstSlot == null || secondSlot == null || firstSlot.arrivalDateTime() == null || secondSlot.departureDateTime() == null) {
      return false;
    }

    return firstSlot.arrivalDateTime()
                .plus(minConnectionTime)
                .isBefore(secondSlot.departureDateTime());
  }
}
