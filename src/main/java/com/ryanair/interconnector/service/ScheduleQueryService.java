package com.ryanair.interconnector.service;

import com.ryanair.interconnector.dto.FlightSlot;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;

public interface ScheduleQueryService {

    Flux<FlightSlot> findFlightSlots(String from, String to, LocalDateTime start, LocalDateTime end);
}
