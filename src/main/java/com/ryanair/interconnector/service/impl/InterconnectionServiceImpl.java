package com.ryanair.interconnector.service.impl;

import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnector.dto.FlightSlot;
import com.ryanair.interconnector.mapping.ConnectionMapper;
import com.ryanair.interconnector.service.InterconnectionService;
import com.ryanair.interconnector.service.RouteQueryService;
import com.ryanair.interconnector.service.ScheduleQueryService;
import com.ryanair.interconnector.validation.FlightConnectionValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class InterconnectionServiceImpl implements InterconnectionService {

  private final ScheduleQueryService scheduleQueryService;
  private final RouteQueryService routeQueryService;
  private final ConnectionMapper connectionMapper;
  private final List<FlightConnectionValidator>  flightConnectionValidators;

  @Autowired
  public InterconnectionServiceImpl(
      ScheduleQueryService scheduleQueryService,
      RouteQueryService routeQueryService,
      ConnectionMapper connectionMapper,
      List<FlightConnectionValidator> flightConnectionValidators
  ) {
    this.scheduleQueryService = scheduleQueryService;
    this.routeQueryService = routeQueryService;
    this.connectionMapper = connectionMapper;
    this.flightConnectionValidators = flightConnectionValidators;
  }

  @Override
  public Flux<Connection> findInterconnections(
      String departure,
      String arrival,
      LocalDateTime departureDateTime,
      LocalDateTime arrivalDateTime) {

    return Flux.merge(
        getSingleLegConnections(departure, arrival, departureDateTime, arrivalDateTime),
        getMultiLegConnections(departure, arrival, departureDateTime, arrivalDateTime)
    );
  }

  private Flux<Connection> getSingleLegConnections(
      String departure,
      String arrival,
      LocalDateTime departureDateTime,
      LocalDateTime arrivalDateTime) {
    return routeQueryService.existsDirectRoute(departure, arrival)
        .doOnNext((valid) -> log.atInfo().setMessage("Single leg connections from {} to {} are {}")
            .addArgument(departure)
            .addArgument(arrival)
            .addArgument(valid ? "EXISTING" : "NOT EXISTING")
            .log())
        .flatMapMany(valid ->
            valid
                ? scheduleQueryService.findFlightSlots(departure, arrival, departureDateTime, arrivalDateTime)
                : Flux.empty())
        .flatMap(slot -> Mono.justOrEmpty(connectionMapper.toSingleLegConnection(departure, arrival, slot)));
  }

  private Flux<Connection> getMultiLegConnections(
      String departure,
      String arrival,
      LocalDateTime minimumDepartureTime,
      LocalDateTime maximumArrivalTime) {

    return routeQueryService.intermediateAirports(departure, arrival)
        .doOnNext(set -> log.atInfo().setMessage("Finding multi-leg connections from {} to {} with common airports: {}")
            .addArgument(departure)
            .addArgument(arrival)
            .addArgument(set.size())
            .log())
        .flatMapMany(Flux::fromIterable)
        .flatMap(intermediate ->
            buildConnectionsVia(departure, intermediate, arrival, minimumDepartureTime, maximumArrivalTime));
  }

  private Flux<Connection> buildConnectionsVia(
      String departure,
      String intermediate,
      String arrival,
      LocalDateTime minimumDepartureTime,
      LocalDateTime maximumArrivalTime) {

    return Mono.zip(
            scheduleQueryService.findFlightSlots(departure, intermediate, minimumDepartureTime, maximumArrivalTime).collectList(),
            scheduleQueryService.findFlightSlots(intermediate, arrival,  minimumDepartureTime, maximumArrivalTime).collectList())
        .flatMapMany(firstAndSecondSlots -> Flux.fromIterable(firstAndSecondSlots.getT1())
            .flatMap(firstSlot -> Flux.fromIterable(firstAndSecondSlots.getT2())
                .filter(secondSlot -> isValidConnection(firstSlot, secondSlot))
                .flatMap(secondSlot -> Mono.justOrEmpty(connectionMapper.toMultiLegConnection(departure, intermediate, arrival, firstSlot, secondSlot)))));
  }


  private boolean isValidConnection(FlightSlot firstSlot, FlightSlot secondSlot) {
    return flightConnectionValidators.stream().allMatch(validator -> validator.isValidConnection(firstSlot, secondSlot));
  }
}


