package com.ryanair.interconnector.service.impl;

import com.ryanair.interconnectingflights.external.model.ScheduleResponse;
import com.ryanair.interconnector.client.CachedSchedulesProvider;
import com.ryanair.interconnector.dto.FlightSlot;
import com.ryanair.interconnector.mapping.ScheduleMapper;
import com.ryanair.interconnector.service.ScheduleQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Service
public class ScheduleQueryServiceImpl implements ScheduleQueryService {

  private final CachedSchedulesProvider schedulesProvider;
  private final ScheduleMapper scheduleMapper;
  private final Executor externalApiExecutor;

  @Autowired
  public ScheduleQueryServiceImpl(CachedSchedulesProvider schedulesProvider, ScheduleMapper scheduleMapper, @Qualifier("externalApiExecutor") Executor externalApiExecutor) {
    this.schedulesProvider = schedulesProvider;
    this.scheduleMapper = scheduleMapper;
    this.externalApiExecutor = externalApiExecutor;
  }

  @Override
  public CompletableFuture<List<FlightSlot>> findFlightSlots(String from, String to, LocalDateTime start,
      LocalDateTime end) {
    YearMonth startMonth = YearMonth.from(start);
    YearMonth endMonth = YearMonth.from(end);

    List<YearMonth> months =
        Stream.iterate(startMonth, month -> !month.isAfter(endMonth), month -> month.plusMonths(1)).toList();

    List<CompletableFuture<List<FlightSlot>>> futures =
        months.stream().map(month -> fetchMonthAsync(from, to, month, start, end)).toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream().flatMap(f -> f.join().stream()).toList());
  }

  private CompletableFuture<List<FlightSlot>> fetchMonthAsync(String from, String to, YearMonth month,
      LocalDateTime start, LocalDateTime end) {
    return CompletableFuture.supplyAsync(() -> {
      ScheduleResponse response = schedulesProvider.getScheduleCached(from, to, month.getYear(), month.getMonthValue());
      List<FlightSlot> slots = scheduleMapper.toFlightSlots(month.getYear(), response);
      return slots.stream()
          .filter(slot -> start.isBefore(slot.departureDateTime()) && end.isAfter(slot.arrivalDateTime()))
          .toList();
    }, externalApiExecutor);
  }


}



