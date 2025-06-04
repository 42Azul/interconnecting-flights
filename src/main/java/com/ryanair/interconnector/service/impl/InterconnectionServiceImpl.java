package com.ryanair.interconnector.service.impl;

import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnector.dto.FlightSlot;
import com.ryanair.interconnector.mapping.ConnectionMapper;
import com.ryanair.interconnector.service.InterconnectionService;
import com.ryanair.interconnector.service.RouteQueryService;
import com.ryanair.interconnector.service.ScheduleQueryService;
import com.ryanair.interconnector.validation.FlightConnectionValidator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class InterconnectionServiceImpl implements InterconnectionService {

  private final ScheduleQueryService scheduleQueryService;
  private final RouteQueryService routeQueryService;
  private final ConnectionMapper connectionMapper;
  private final List<FlightConnectionValidator> flightConnectionValidators;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "List is injected by Spring and not exposed"
  )
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
  public CompletableFuture<List<Connection>> findInterconnections(
      String departure,
      String arrival,
      LocalDateTime departureDateTime,
      LocalDateTime arrivalDateTime) {

    CompletableFuture<List<Connection>> singleLegFut = getSingleLegConnections(
        departure,
        arrival,
        departureDateTime,
        arrivalDateTime);

    CompletableFuture<List<Connection>> multiLegFut = getMultiLegConnections(
        departure,
        arrival,
        departureDateTime,
        arrivalDateTime);

    // Combine both results preserving the order: direct first, then interconnected
    return singleLegFut.thenCombine(multiLegFut, (single, multi) -> {
      List<Connection> combined = new ArrayList<>(single.size() + multi.size());
      combined.addAll(single);
      combined.addAll(multi);
      return combined;
    });
  }

  private CompletableFuture<List<Connection>> getSingleLegConnections(
      String departure,
      String arrival,
      LocalDateTime departureDateTime,
      LocalDateTime arrivalDateTime) {
    return routeQueryService.existsDirectRoute(departure, arrival)
        .thenApply(valid -> {
              log.atInfo().setMessage("Single leg connections from {} to {} are {}")
                  .addArgument(departure)
                  .addArgument(arrival)
                  .addArgument(valid ? "EXISTING" : "NOT EXISTING")
                  .log();
              return valid;
            }
        )
        .thenCompose(valid ->
            valid
                ? scheduleQueryService.findFlightSlots(departure, arrival, departureDateTime, arrivalDateTime)
                : CompletableFuture.completedFuture(Collections.emptyList()))
        .thenApply(slots ->
            slots.stream()
                .map(slot -> connectionMapper.toSingleLegConnection(departure, arrival, slot))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList());
  }

  private CompletableFuture<List<Connection>> getMultiLegConnections(
      String departure,
      String arrival,
      LocalDateTime minimumDepartureTime,
      LocalDateTime maximumArrivalTime) {

    return routeQueryService.intermediateAirports(departure, arrival)
        .thenApply(set -> {
          log.atInfo().setMessage("Finding multi-leg connections from {} to {} with common airports: {}")
              .addArgument(departure)
              .addArgument(arrival)
              .addArgument(set.size())
              .log();
          return set;
        })
        .thenCompose(set -> {
          List<CompletableFuture<List<Connection>>> futures = set.stream()
              .map(intermediate -> buildConnectionsVia(departure, intermediate, arrival, minimumDepartureTime,
                  maximumArrivalTime))
              .toList();

          return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
              .thenApply(v -> futures.stream()
                  .flatMap(f -> f.join().stream())
                  .toList());
        });
  }

  private CompletableFuture<List<Connection>> buildConnectionsVia(
      String departure,
      String intermediate,
      String arrival,
      LocalDateTime minimumDepartureTime,
      LocalDateTime maximumArrivalTime) {

    CompletableFuture<List<FlightSlot>> firstLegFut = scheduleQueryService.findFlightSlots(
        departure, intermediate, minimumDepartureTime, maximumArrivalTime);

    CompletableFuture<List<FlightSlot>> secondLegFut = scheduleQueryService.findFlightSlots(
        intermediate, arrival, minimumDepartureTime, maximumArrivalTime);

    return firstLegFut.thenCombine(secondLegFut, (firstSlots, secondSlots) -> {
      List<Connection> connections = new ArrayList<>();
      for (FlightSlot firstSlot : firstSlots) {
        for (FlightSlot secondSlot : secondSlots) {
          if (isValidConnection(firstSlot, secondSlot)) {
            connectionMapper.toMultiLegConnection(
                    departure, intermediate, arrival, firstSlot, secondSlot)
                .ifPresent(connections::add);
          }
        }
      }
      return connections;
    });
  }

  private boolean isValidConnection(FlightSlot firstSlot, FlightSlot secondSlot) {
    return flightConnectionValidators.stream()
        .allMatch(validator -> validator.isValidConnection(firstSlot, secondSlot));
  }
}


