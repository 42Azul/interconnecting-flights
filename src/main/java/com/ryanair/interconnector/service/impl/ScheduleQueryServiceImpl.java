package com.ryanair.interconnector.service.impl;

import com.ryanair.interconnectingflights.external.model.ScheduleResponse;
import com.ryanair.interconnector.client.SchedulesClient;
import com.ryanair.interconnector.dto.FlightSlot;
import com.ryanair.interconnector.mapping.ScheduleMapper;
import com.ryanair.interconnector.service.ScheduleQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.stream.Stream;

@Service
public class ScheduleQueryServiceImpl implements ScheduleQueryService {

  private final SchedulesClient schedulesClient;
  private final ScheduleMapper scheduleMapper;

  @Autowired
  public ScheduleQueryServiceImpl(SchedulesClient schedulesClient, ScheduleMapper scheduleMapper) {
    this.schedulesClient = schedulesClient;
    this.scheduleMapper = scheduleMapper;
  }

  @Override
  public Flux<FlightSlot> findFlightSlots(String from, String to, LocalDateTime start, LocalDateTime end) {
    YearMonth startMonth = YearMonth.from(start);
    YearMonth endMonth = YearMonth.from(end);

    return Flux.fromStream(() ->
            Stream.iterate(startMonth, month -> !month.isAfter(endMonth), month -> month.plusMonths(1))
        )
        .flatMap(month ->
            schedulesClient.getScheduleAsync(from, to, month.getYear(), month.getMonthValue())
                .flatMapMany(response ->
                    Flux.fromIterable(scheduleMapper.toFlightSlots(month.getYear(), response))
                        .filter(slot -> start.isBefore(slot.departureDateTime()) && end.isAfter(slot.arrivalDateTime()))
                )
        );
  }

}

