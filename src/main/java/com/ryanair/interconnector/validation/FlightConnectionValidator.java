package com.ryanair.interconnector.validation;

import com.ryanair.interconnector.dto.FlightSlot;

/**
 * Interface for validating flight connections.
 * This interface defines a method to check if a connection between two flight slots is valid depending on specific criteria.
 */
public interface FlightConnectionValidator {

  boolean isValidConnection(FlightSlot firstSlot, FlightSlot secondSlot);
}
