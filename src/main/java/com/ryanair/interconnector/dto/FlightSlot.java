package com.ryanair.interconnector.dto;

import java.time.LocalDateTime;

/**
 * Represents a flight slot with departure and arrival times.
 */

public record FlightSlot(LocalDateTime departureDateTime,
                         LocalDateTime arrivalDateTime) { }
