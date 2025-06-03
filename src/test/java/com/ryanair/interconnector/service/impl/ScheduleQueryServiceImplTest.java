package com.ryanair.interconnector.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ryanair.interconnectingflights.external.model.ScheduleResponse;
import com.ryanair.interconnector.client.SchedulesClient;
import com.ryanair.interconnector.dto.FlightSlot;
import com.ryanair.interconnector.mapping.ScheduleMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ScheduleQueryServiceImplTest {

  private static final String FROM = "DUB";
  private static final String TO = "WRO";

  private static final YearMonth DEC = YearMonth.of(2022, 12);
  private static final YearMonth JAN = YearMonth.of(2023, 1);
  private static final YearMonth FEB = YearMonth.of(2023, 2);
  private static final LocalDateTime BASE = LocalDateTime.of(2023, 1, 10, 8, 0);
  private static final LocalDateTime RANGE_START = BASE;
  private static final LocalDateTime RANGE_END = BASE.plusDays(10).withHour(18);

  @Mock
  SchedulesClient client;

  @Mock
  ScheduleMapper mapper;

  @InjectMocks
  ScheduleQueryServiceImpl service;

  private void stubMonth(YearMonth ym, FlightSlot... slots) {
    ScheduleResponse resp = mock(ScheduleResponse.class);
    if(slots.length == 0) {
      when(client.getScheduleAsync(FROM, TO, ym.getYear(), ym.getMonthValue()))
          .thenReturn(Mono.empty());
    } else {
      when(client.getScheduleAsync(FROM, TO, ym.getYear(), ym.getMonthValue())).thenReturn(Mono.just(resp));
      when(mapper.toFlightSlots(ym.getYear(), resp)).thenReturn(List.of(slots));
    }
  }

  @Nested
  class SingleMonth {

    @Test
    void shouldReturnOnlyInRangeSlots() {
      // Arrange
      FlightSlot valid = new FlightSlot(RANGE_START.plusDays(2), RANGE_END.minusDays(2));
      FlightSlot out = new FlightSlot(RANGE_START.minusDays(2), RANGE_END.minusDays(10));
      stubMonth(JAN, valid, out);

      // Act
      Flux<FlightSlot> result = service.findFlightSlots(FROM, TO, RANGE_START, RANGE_END);

      // Assert
      StepVerifier.create(result).expectNext(valid).verifyComplete();

      // Verify interactions
      verify(client).getScheduleAsync(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verifyNoMoreInteractions(client);
    }

    @Test
    void shouldExcludeBoundarySlots() {
      // Arrange
      FlightSlot atStart = new FlightSlot(RANGE_START, RANGE_START.plusHours(2));
      FlightSlot atEnd = new FlightSlot(RANGE_END.minusHours(2), RANGE_END);
      FlightSlot inRange = new FlightSlot(RANGE_START.plusDays(1), RANGE_END.minusDays(1));
      stubMonth(JAN, atStart, atEnd, inRange);

      // Act
      Flux<FlightSlot> result = service.findFlightSlots(FROM, TO, RANGE_START, RANGE_END);

      // Assert
      StepVerifier.create(result).expectNext(inRange).verifyComplete();

      // Verify interactions
      verify(client).getScheduleAsync(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verifyNoMoreInteractions(client);
    }

    @Test
    void shouldReturnEmptyWhenNoSlots() {
      // Arrange
      stubMonth(JAN);

      // Act & Assert
      StepVerifier.create(service.findFlightSlots(FROM, TO, RANGE_START, RANGE_END)).verifyComplete();

      // Verify interactions
      verify(client).getScheduleAsync(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verifyNoMoreInteractions(client);
    }
  }

  @Nested
  class MultiMonth {

    @Test
    void shouldAggregateAcrossMonths() {
      // Arrange
      LocalDateTime midFebruary = FEB.atDay(15).atTime(12, 0);
      FlightSlot january = new FlightSlot(RANGE_START.plusDays(5), RANGE_START.plusDays(5).plusHours(2));
      FlightSlot februaryValid = new FlightSlot(FEB.atDay(10).atTime(10, 0), FEB.atDay(10).atTime(12, 0));
      FlightSlot februaryInvalid = new FlightSlot(midFebruary.minusDays(1), midFebruary.plusHours(2));
      stubMonth(JAN, january);
      stubMonth(FEB, februaryValid, februaryInvalid);

      // Act
      Flux<FlightSlot> result = service.findFlightSlots(FROM, TO, RANGE_START, midFebruary);

      // Assert
      StepVerifier.create(result).expectNext(january, februaryValid).verifyComplete();

      // Verify interactions so for example march is not called as it is out of range
      verify(client).getScheduleAsync(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verify(client).getScheduleAsync(FROM, TO, FEB.getYear(), FEB.getMonthValue());
      verifyNoMoreInteractions(client);
    }

    @Test
    void shouldHandleDifferentYears(){
      // Arrange
      LocalDateTime start = DEC.atDay(20).atTime(10, 0);
      LocalDateTime end = JAN.atDay(6).atTime(18, 0);

      FlightSlot decValid = new FlightSlot(start.plusDays(1), start.plusDays(1).plusHours(2));
      FlightSlot janValid = new FlightSlot(end.minusDays(1), end.minusDays(1).plusHours(2));
      FlightSlot janInvalid = new FlightSlot(end.minusHours(2), end.plusDays(1));

      stubMonth(DEC, decValid);
      stubMonth(JAN, janValid, janInvalid);

      // Act
      Flux<FlightSlot> result = service.findFlightSlots(FROM, TO, start, end);

      // Assert
      StepVerifier.create(result).expectNext(decValid, janValid).verifyComplete();

      // Verify interactions
      verify(client).getScheduleAsync(FROM, TO, DEC.getYear(), DEC.getMonthValue());
      verify(client).getScheduleAsync(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verifyNoMoreInteractions(client);
    }
  }
}
