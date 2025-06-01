package com.ryanair.interconnector.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnector.dto.FlightSlot;
import com.ryanair.interconnector.mapping.ConnectionMapper;
import com.ryanair.interconnector.service.RouteQueryService;
import com.ryanair.interconnector.service.ScheduleQueryService;
import com.ryanair.interconnector.validation.FlightConnectionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class InterconnectionServiceImplTest {

  @Mock ScheduleQueryService scheduleQueryService;
  @Mock RouteQueryService routeQueryService;
  @Mock ConnectionMapper connectionMapper;
  @Mock FlightConnectionValidator validator;
  @Spy
  List<FlightConnectionValidator> validatorList = new ArrayList<>();

  @InjectMocks InterconnectionServiceImpl service;

  static final String ORIGIN = "DUB";
  static final String DEST = "WRO";
  static final String MID = "STN";

  static final LocalDateTime SINCE = LocalDateTime.of(2023, 6, 1, 10, 0);
  static final LocalDateTime UNTIL = LocalDateTime.of(2023, 6, 1, 18, 0);

  FlightSlot slotDirect;
  FlightSlot slotFirstLeg;
  FlightSlot slotSecondLeg;
  Connection directConn;
  Connection multiConn;

  @BeforeEach
  void setUpSlotsAndConnections() {
    slotDirect = new FlightSlot(SINCE, SINCE.plusHours(2));
    slotFirstLeg = new FlightSlot(SINCE.minusHours(1), SINCE);
    slotSecondLeg = new FlightSlot(SINCE.plusHours(3), UNTIL);

    directConn = new Connection();
    directConn.setStops(0);
    multiConn = new Connection();
    multiConn.setStops(1);
  }

  private void mockValidSingleLegSetup(){
    when(routeQueryService.existsDirectRoute(ORIGIN, DEST)).thenReturn(Mono.just(true));
    when(scheduleQueryService.findFlightSlots(ORIGIN, DEST, SINCE, UNTIL)).thenReturn(Flux.just(slotDirect));
    when(connectionMapper.toSingleLegConnection(ORIGIN, DEST, slotDirect)).thenReturn(Optional.of(directConn));
  }

  private void mockValidMultiLegSetup() {
    when(routeQueryService.intermediateAirports(ORIGIN, DEST)).thenReturn(Mono.just(Set.of(MID)));
    when(scheduleQueryService.findFlightSlots(ORIGIN, MID, SINCE, UNTIL)).thenReturn(Flux.just(slotFirstLeg));
    when(scheduleQueryService.findFlightSlots(MID, DEST, SINCE, UNTIL)).thenReturn(Flux.just(slotSecondLeg));
    when(connectionMapper.toMultiLegConnection(ORIGIN, MID, DEST, slotFirstLeg, slotSecondLeg)).thenReturn(Optional.of(multiConn));
  }

  @Nested
  class SingleLegPath {

    @BeforeEach
    void setUp() {
      when(routeQueryService.intermediateAirports(ORIGIN, DEST)).thenReturn(Mono.just(Set.of()));
    }

    @Test
    void shouldReturnSingleLegConnectionWhenValidRouteExists() {
      // Arrange
      mockValidSingleLegSetup();

      // Act
      Flux<Connection> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      StepVerifier.create(result)
          .expectNext(directConn)
          .verifyComplete();

      // Verify
      verify(connectionMapper).toSingleLegConnection(ORIGIN, DEST, slotDirect);
      verify(scheduleQueryService).findFlightSlots(ORIGIN, DEST, SINCE, UNTIL);
      verifyNoMoreInteractions(connectionMapper);
      verifyNoMoreInteractions(scheduleQueryService);
    }

    @Test
    void shouldReturnEmptyWhenNoValidSlotsFound() {
      // Arrange
      when(routeQueryService.existsDirectRoute(ORIGIN, DEST)).thenReturn(Mono.just(true));
      when(scheduleQueryService.findFlightSlots(ORIGIN, DEST, SINCE, UNTIL)).thenReturn(Flux.empty());

      // Act
      Flux<Connection> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      StepVerifier.create(result)
          .expectComplete()
          .verify();

      // Verify
      verify(scheduleQueryService).findFlightSlots(ORIGIN, DEST, SINCE, UNTIL);
      verifyNoInteractions(connectionMapper);
      verifyNoMoreInteractions(scheduleQueryService);
    }

    @Test
    void shouldFilterOutSingleLegIfRouteDoesNotExist() {
      // Arrange
      when(routeQueryService.existsDirectRoute(ORIGIN, DEST)).thenReturn(Mono.just(false));

      // Act
      Flux<Connection> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      StepVerifier.create(result)
          .expectComplete()
          .verify();

      // Verify
      verifyNoInteractions(scheduleQueryService);
      verifyNoInteractions(connectionMapper);
    }
  }

  @Nested
  class MultiLegPath {

    @BeforeEach
    void setUp() {
      when(routeQueryService.existsDirectRoute(ORIGIN, DEST)).thenReturn(Mono.just(false));
      validatorList.clear();
      validatorList.add(validator);
    }

    @Test
    void shouldReturnMultiLegIfIntermediateAirportAndValidSlotsFound() {
      // Arrange
      mockValidMultiLegSetup();
      when(validator.isValidConnection(any(), any())).thenReturn(true);

      // Act
      Flux<Connection> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      StepVerifier.create(result)
          .expectNext(multiConn)
          .verifyComplete();

      // Verify
      verify(connectionMapper).toMultiLegConnection(ORIGIN, MID, DEST, slotFirstLeg, slotSecondLeg);
    }

    @Test
    void shouldFilterOutMultiLegIfValidatorReturnsFalse() {
      // Arrange
      when(routeQueryService.intermediateAirports(ORIGIN, DEST)).thenReturn(Mono.just(Set.of(MID)));
      when(scheduleQueryService.findFlightSlots(ORIGIN, MID, SINCE, UNTIL)).thenReturn(Flux.just(slotFirstLeg));
      when(scheduleQueryService.findFlightSlots(MID, DEST, SINCE, UNTIL)).thenReturn(Flux.just(slotSecondLeg));
      when(validator.isValidConnection(any(), any())).thenReturn(false);

      // Act
      Flux<Connection> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      StepVerifier.create(result)
          .expectComplete()
          .verify();

      // Verify
      verifyNoInteractions(connectionMapper);
    }

    @Test
    void returnsEmptyWhenNoIntermediateAirports() {
      // Arrange
      when(routeQueryService.intermediateAirports(ORIGIN, DEST)).thenReturn(Mono.just(Set.of()));

      // Act
      Flux<Connection> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      StepVerifier.create(result)
          .expectComplete()
          .verify();

      // Verify
      verify(scheduleQueryService, never()).findFlightSlots(any(), any(), any(), any());
    }
  }

  @Nested
  class MixedScenarios {

    @BeforeEach
    void setUp() {
      validatorList.clear();
      validatorList.add(validator);
    }

    @Test
    void returnsBothSingleAndMultiLeg() {
      // Arrange
      mockValidSingleLegSetup();
      mockValidMultiLegSetup();
      when(validator.isValidConnection(any(), any())).thenReturn(true);

      // Act
      Flux<Connection> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      StepVerifier.create(result.collectList())
          .expectNextMatches(connections -> connections.size() == 2 &&
              connections.contains(directConn) &&
              connections.contains(multiConn))
          .verifyComplete();

      // Verify
      verify(connectionMapper).toSingleLegConnection(eq(ORIGIN), eq(DEST), any());
      verify(connectionMapper).toMultiLegConnection(eq(ORIGIN), eq(MID), eq(DEST), any(), any());
    }
  }
}
