package com.ryanair.interconnector.service;

import com.ryanair.interconnectingflights.model.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service that abstracts finding connections from a departure airport to another arrival one in a given range of time.
 * It wraps response in CompletableFuture to improve async handling.
 */
public interface InterconnectionService {

  /**
   * Finds interconnections between two airports within a specified time range.
   * @param departure Origin airport code (e.g., "DUB" for Dublin)
   * @param arrival Destination airport code (e.g., "LON" for London)
   * @param departureDateTime LocalDateTime of departure (e.g., 2023-10-01T10:00:00)
   * @param arrivalDateTime LocalDateTime of arrival (e.g., 2023-10-01T12:00:00)
   * @return CompletableFuture containing a list of Connection objects representing the interconnections found.
   */
  CompletableFuture<List<Connection>> findInterconnections(String departure, String arrival, LocalDateTime departureDateTime,
      LocalDateTime arrivalDateTime);

}
