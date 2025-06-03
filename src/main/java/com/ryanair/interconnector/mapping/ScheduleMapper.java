package com.ryanair.interconnector.mapping;

import com.ryanair.interconnectingflights.external.model.Flight;
import com.ryanair.interconnectingflights.external.model.ScheduleResponse;
import com.ryanair.interconnector.dto.FlightSlot;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class ScheduleMapper {

    public List<FlightSlot> toFlightSlots(int year, ScheduleResponse response) {
        if (response == null || response.getMonth() == null || response.getDays() == null) {
            return List.of();
        }

        int month = response.getMonth();

        return response.getDays().stream()
            .filter(day -> day.getFlights() != null && day.getDay() != null)
            .flatMap(day -> day.getFlights().stream()
                .map(flight -> toSlot(year, month, day.getDay(), flight))
                .flatMap(Optional::stream)
            )
            .toList();
    }

    private Optional<FlightSlot> toSlot(int year, int month, int day, @NotNull Flight flight) {
        if (flight.getDepartureTime() == null || flight.getArrivalTime() == null) {
            return Optional.empty();
        }

        LocalDate date = LocalDate.of(year, month, day);
        return Optional.of(new FlightSlot(
            LocalDateTime.of(date, flight.getDepartureTime()),
            LocalDateTime.of(date, flight.getArrivalTime())
        ));
    }
}
