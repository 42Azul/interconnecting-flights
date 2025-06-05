package com.ryanair.interconnector.validation.connection;

import com.ryanair.interconnector.dto.FlightSlot;

/**
 * Interface for validating flight connections.
 * It defines a method to check if a connection between two flight slots is valid depending on specific criteria.
 */
public interface FlightConnectionValidator {

  /**
   * Checks if a connection between two flight slots is valid depending on specific criteria.
   * @param firstSlot
   * @param secondSlot
   * @return true if the connection is valid, false otherwise
   */
  boolean isValidConnection(FlightSlot firstSlot, FlightSlot secondSlot);
}
