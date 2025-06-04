package com.ryanair.interconnector.service;

import com.ryanair.interconnector.dto.FlightSlot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ScheduleQueryService {

    CompletableFuture<List<FlightSlot>> findFlightSlots(String from, String to, LocalDateTime start, LocalDateTime end);
}
