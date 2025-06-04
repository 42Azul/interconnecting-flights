package com.ryanair.interconnector.service;

import com.ryanair.interconnector.dto.FlightSlot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for querying flight schedules. It provides methods to find direct available flight slots between a given date and time range
 * in specific departure and arrival locations.
 */
public interface ScheduleQueryService {

    /**
     * Finds direct available flight slots between two locations within a specified date and time range.
     * @param from Origin airport code in IATA format (e.g., "DUB" for Dublin).
     * @param to Destination airport code in IATA format (e.g., "LON" for London).
     * @param start LocalDateTime representing the start of the search range (e.g , 2023-10-01T08:00:00).
     * @param end LocalDateTime representing the end of the search range (e.g , 2023-10-01T12:00:00).
     * @return A CompletableFuture that resolves to a list of FlightSlot objects representing the available flight slots.
     */
    CompletableFuture<List<FlightSlot>> findFlightSlots(String from, String to, LocalDateTime start, LocalDateTime end);
}
