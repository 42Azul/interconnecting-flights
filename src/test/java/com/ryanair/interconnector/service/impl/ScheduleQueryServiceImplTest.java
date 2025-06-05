package com.ryanair.interconnector.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ryanair.interconnectingflights.external.model.ScheduleResponse;
import com.ryanair.interconnector.client.CachedSchedulesProvider;
import com.ryanair.interconnector.dto.FlightSlot;
import com.ryanair.interconnector.mapping.ScheduleMapper;
import com.ryanair.interconnector.testutils.DirectExecutor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

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
  ScheduleMapper mapper;

  @Mock
  CachedSchedulesProvider schedulesProvider;

  // Using direct execution for the unit tests, as we do not want async checks here
  @Spy
  Executor directExecutor = new DirectExecutor();

  @InjectMocks
  ScheduleQueryServiceImpl service;

  private void stubMonth(YearMonth ym, FlightSlot... slots) {
    ScheduleResponse resp = mock(ScheduleResponse.class);
    if (slots.length == 0) {
      when(schedulesProvider.getScheduleCached(FROM, TO, ym.getYear(), ym.getMonthValue())).thenReturn(null);
    } else {
      when(schedulesProvider.getScheduleCached(FROM, TO, ym.getYear(), ym.getMonthValue())).thenReturn(resp);
      when(mapper.toFlightSlots(ym.getYear(), resp)).thenReturn(List.of(slots));
    }
  }

  @Nested
  class SingleMonth {

    @Test
    void shouldReturnOnlyInRangeSlots() throws ExecutionException, InterruptedException {
      // Arrange
      FlightSlot valid = new FlightSlot(RANGE_START.plusDays(2), RANGE_END.minusDays(2));
      FlightSlot out = new FlightSlot(RANGE_START.minusDays(2), RANGE_END.minusDays(10));
      stubMonth(JAN, valid, out);

      // Act
      CompletableFuture<List<FlightSlot>> result = service.findFlightSlots(FROM, TO, RANGE_START, RANGE_END);

      // Assert
      List<FlightSlot> resultList = result.get();
      assertEquals(1, resultList.size());
      assertEquals(valid, resultList.get(0));

      // Verify interactions
      verify(schedulesProvider).getScheduleCached(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verifyNoMoreInteractions(schedulesProvider);
    }

    @Test
    void shouldIncludeBoundarySlots() throws ExecutionException, InterruptedException {
      // Arrange
      FlightSlot atStart = new FlightSlot(RANGE_START, RANGE_START.plusHours(2));
      FlightSlot atEnd = new FlightSlot(RANGE_END.minusHours(2), RANGE_END);
      stubMonth(JAN, atStart, atEnd);

      // Act
      CompletableFuture<List<FlightSlot>> result = service.findFlightSlots(FROM, TO, RANGE_START, RANGE_END);

      // Assert
      List<FlightSlot> resultList = result.get();
      assertEquals(2, resultList.size());
      assertTrue(resultList.contains(atStart));
      assertTrue(resultList.contains(atEnd));

      // Verify interactions
      verify(schedulesProvider).getScheduleCached(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verifyNoMoreInteractions(schedulesProvider);
    }

    @Test
    void shouldReturnEmptyWhenNoSlots() throws ExecutionException, InterruptedException {
      // Arrange
      stubMonth(JAN);

      // Act
      CompletableFuture<List<FlightSlot>> result = service.findFlightSlots(FROM, TO, RANGE_START, RANGE_END);

      // Assert
      List<FlightSlot> resultList = result.get();
      assertTrue(resultList.isEmpty());

      // Verify interactions
      verify(schedulesProvider).getScheduleCached(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verifyNoMoreInteractions(schedulesProvider);
    }
  }

  @Nested
  class MultiMonth {

    @Test
    void shouldAggregateAcrossMonths() throws ExecutionException, InterruptedException {
      // Arrange
      LocalDateTime midFebruary = FEB.atDay(15).atTime(12, 0);
      FlightSlot january = new FlightSlot(RANGE_START.plusDays(5), RANGE_START.plusDays(5).plusHours(2));
      FlightSlot februaryValid = new FlightSlot(FEB.atDay(10).atTime(10, 0), FEB.atDay(10).atTime(12, 0));
      FlightSlot februaryInvalid = new FlightSlot(midFebruary.minusDays(1), midFebruary.plusHours(2));
      stubMonth(JAN, january);
      stubMonth(FEB, februaryValid, februaryInvalid);

      // Act
      CompletableFuture<List<FlightSlot>> result = service.findFlightSlots(FROM, TO, RANGE_START, midFebruary);

      // Assert
      List<FlightSlot> resultList = result.get();
      assertEquals(2, resultList.size());
      assertTrue(resultList.contains(january));
      assertTrue(resultList.contains(februaryValid));

      // Verify interactions so for example march is not called as it is out of range
      verify(schedulesProvider).getScheduleCached(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verify(schedulesProvider).getScheduleCached(FROM, TO, FEB.getYear(), FEB.getMonthValue());
      verifyNoMoreInteractions(schedulesProvider);
    }

    @Test
    void shouldHandleDifferentYears() throws ExecutionException, InterruptedException {
      // Arrange
      LocalDateTime start = DEC.atDay(20).atTime(10, 0);
      LocalDateTime end = JAN.atDay(6).atTime(18, 0);

      FlightSlot decValid = new FlightSlot(start.plusDays(1), start.plusDays(1).plusHours(2));
      FlightSlot janValid = new FlightSlot(end.minusDays(1), end.minusDays(1).plusHours(2));
      FlightSlot janInvalid = new FlightSlot(end.minusHours(2), end.plusDays(1));

      stubMonth(DEC, decValid);
      stubMonth(JAN, janValid, janInvalid);

      // Act
      CompletableFuture<List<FlightSlot>> result = service.findFlightSlots(FROM, TO, start, end);

      // Assert
      List<FlightSlot> resultList = result.get();
      assertEquals(2, resultList.size());
      assertTrue(resultList.contains(decValid));

      // Verify interactions
      verify(schedulesProvider).getScheduleCached(FROM, TO, DEC.getYear(), DEC.getMonthValue());
      verify(schedulesProvider).getScheduleCached(FROM, TO, JAN.getYear(), JAN.getMonthValue());
      verifyNoMoreInteractions(schedulesProvider);
    }
  }
}
