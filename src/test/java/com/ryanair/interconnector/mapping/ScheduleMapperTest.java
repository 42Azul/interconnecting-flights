package com.ryanair.interconnector.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ryanair.interconnectingflights.external.model.DaySchedule;
import com.ryanair.interconnectingflights.external.model.Flight;
import com.ryanair.interconnectingflights.external.model.ScheduleResponse;
import com.ryanair.interconnector.dto.FlightSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

class ScheduleMapperTest {

  private final ScheduleMapper mapper = new ScheduleMapper();

  private static final int YEAR = 2023;
  private static final int MONTH = 6;
  private static final int DAY = 5;

  private static final LocalTime DEP_TIME = LocalTime.of(10, 0);
  private static final LocalTime ARR_TIME = LocalTime.of(12, 30);
  private static final LocalDateTime EXPECTED_DEP = LocalDateTime.of(YEAR, MONTH, DAY, 10, 0);
  private static final LocalDateTime EXPECTED_ARR = LocalDateTime.of(YEAR, MONTH, DAY, 12, 30);

  static List<Flight> invalidFlights() {
    return List.of(
        new Flight(),
        new Flight().departureTime(LocalTime.of(8, 0)),
        new Flight().arrivalTime(LocalTime.of(9, 0))
    );
  }

  private ScheduleResponse getScheduleResponseWithFlights(Flight... flights) {
    DaySchedule day = new DaySchedule().day(DAY).flights(List.of(flights));
    return new ScheduleResponse().month(MONTH).days(List.of(day));
  }

  @Test
  void shouldMapValidFlightSlot() {
    // Arrange
    Flight flight = new Flight().departureTime(DEP_TIME).arrivalTime(ARR_TIME);
    ScheduleResponse response = getScheduleResponseWithFlights(flight);

    // Act
    List<FlightSlot> result = mapper.toFlightSlots(YEAR, response);

    // Assert
    assertEquals(1, result.size());
    FlightSlot slot = result.get(0);
    assertEquals(EXPECTED_DEP, slot.departureDateTime());
    assertEquals(EXPECTED_ARR, slot.arrivalDateTime());
  }

  @Test
  void shouldMapOnlyValidFlights() {
    // Arrange
    Flight validFlight = new Flight().departureTime(DEP_TIME).arrivalTime(ARR_TIME);
    Flight invalidFlight = new Flight();
    ScheduleResponse response = getScheduleResponseWithFlights(validFlight, invalidFlight);

    // Act
    List<FlightSlot> result = mapper.toFlightSlots(YEAR, response);

    // Assert
    assertEquals(1, result.size());
    FlightSlot slot = result.get(0);
    assertEquals(EXPECTED_DEP, slot.departureDateTime());
    assertEquals(EXPECTED_ARR, slot.arrivalDateTime());
  }

  @ParameterizedTest
  @NullSource
  @EmptySource
  void shouldIgnoreNullOrEmptyFlightsList(List<Flight> emptyFlights) {
    // Arrange
    DaySchedule day = new DaySchedule().day(DAY).flights(emptyFlights);
    ScheduleResponse response = new ScheduleResponse().month(MONTH).days(List.of(day));

    // Act
    List<FlightSlot> result = mapper.toFlightSlots(YEAR, response);

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldIgnoreDayWithNullDayValue() {
    // Arrange
    Flight flight = new Flight().departureTime(DEP_TIME).arrivalTime(ARR_TIME);
    DaySchedule day = new DaySchedule().day(null).flights(List.of(flight));
    ScheduleResponse response = new ScheduleResponse().month(MONTH).days(List.of(day));

    // Act
    List<FlightSlot> result = mapper.toFlightSlots(YEAR, response);

    // Assert
    assertTrue(result.isEmpty());
  }

  @ParameterizedTest
  @MethodSource("invalidFlights")
  void shouldIgnoreFlightsWithoutDepartureOrArrivalTime(Flight flight) {
    // Arrange
    DaySchedule day = new DaySchedule().day(DAY).flights(List.of(flight));
    ScheduleResponse response = new ScheduleResponse().month(MONTH).days(List.of(day));

    // Act
    List<FlightSlot> result = mapper.toFlightSlots(YEAR, response);

    // Assert
    assertTrue(result.isEmpty());
  }


  @Test
  void shouldReturnEmptyWhenResponseIsNullOrIncomplete() {
    // Arrange / Act / Assert
    assertTrue(mapper.toFlightSlots(YEAR, null).isEmpty());
    assertTrue(mapper.toFlightSlots(YEAR, new ScheduleResponse().month(null)).isEmpty());
    assertTrue(mapper.toFlightSlots(YEAR, new ScheduleResponse().month(MONTH).days(null)).isEmpty());
  }
}
