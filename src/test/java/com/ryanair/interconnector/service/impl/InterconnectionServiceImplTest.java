package com.ryanair.interconnector.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.ryanair.interconnector.validation.connection.FlightConnectionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@ExtendWith(MockitoExtension.class)
class InterconnectionServiceImplTest {

  @Mock
  ScheduleQueryService scheduleQueryService;
  @Mock
  RouteQueryService routeQueryService;
  @Mock
  ConnectionMapper connectionMapper;
  @Mock
  FlightConnectionValidator validator;
  @Spy
  List<FlightConnectionValidator> validatorList = new ArrayList<>();

  @InjectMocks
  InterconnectionServiceImpl service;

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

  private void mockValidSingleLegSetup() {
    when(routeQueryService.existsDirectRoute(ORIGIN, DEST)).thenReturn(CompletableFuture.completedFuture(true));
    when(scheduleQueryService.findFlightSlots(ORIGIN, DEST, SINCE, UNTIL)).thenReturn(
        CompletableFuture.completedFuture(List.of(slotDirect)));
    when(connectionMapper.toSingleLegConnection(ORIGIN, DEST, slotDirect)).thenReturn(Optional.of(directConn));
  }

  private void mockValidMultiLegSetup() {
    when(routeQueryService.intermediateAirports(ORIGIN, DEST)).thenReturn(
        CompletableFuture.completedFuture(Set.of(MID)));
    when(scheduleQueryService.findFlightSlots(ORIGIN, MID, SINCE, UNTIL)).thenReturn(
        CompletableFuture.completedFuture(List.of(slotFirstLeg)));
    when(scheduleQueryService.findFlightSlots(MID, DEST, SINCE, UNTIL)).thenReturn(
        CompletableFuture.completedFuture(List.of(slotSecondLeg)));
    when(connectionMapper.toMultiLegConnection(ORIGIN, MID, DEST, slotFirstLeg, slotSecondLeg)).thenReturn(
        Optional.of(multiConn));
  }

  @Nested
  class SingleLegPath {

    @BeforeEach
    void setUp() {
      when(routeQueryService.intermediateAirports(ORIGIN, DEST)).thenReturn(
          CompletableFuture.completedFuture(Set.of()));
    }

    @Test
    void shouldReturnSingleLegConnectionWhenValidRouteExists() throws ExecutionException, InterruptedException {
      // Arrange
      mockValidSingleLegSetup();

      // Act
      CompletableFuture<List<Connection>> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      assertEquals(List.of(directConn), result.get());

      // Verify
      verify(connectionMapper).toSingleLegConnection(ORIGIN, DEST, slotDirect);
      verify(scheduleQueryService).findFlightSlots(ORIGIN, DEST, SINCE, UNTIL);
      verifyNoMoreInteractions(connectionMapper);
      verifyNoMoreInteractions(scheduleQueryService);
    }

    @Test
    void shouldReturnEmptyWhenNoValidSlotsFound() throws ExecutionException, InterruptedException {
      // Arrange
      when(routeQueryService.existsDirectRoute(ORIGIN, DEST)).thenReturn(CompletableFuture.completedFuture(true));
      when(scheduleQueryService.findFlightSlots(ORIGIN, DEST, SINCE, UNTIL)).thenReturn(
          CompletableFuture.completedFuture(List.of()));

      // Act
      CompletableFuture<List<Connection>> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      assertEquals(List.of(), result.get());

      // Verify
      verify(scheduleQueryService).findFlightSlots(ORIGIN, DEST, SINCE, UNTIL);
      verifyNoInteractions(connectionMapper);
      verifyNoMoreInteractions(scheduleQueryService);
    }

    @Test
    void shouldFilterOutSingleLegIfRouteDoesNotExist() throws ExecutionException, InterruptedException {
      // Arrange
      when(routeQueryService.existsDirectRoute(ORIGIN, DEST)).thenReturn(CompletableFuture.completedFuture(false));

      // Act
      CompletableFuture<List<Connection>> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      assertEquals(List.of(), result.get());

      // Verify
      verifyNoInteractions(scheduleQueryService);
      verifyNoInteractions(connectionMapper);
    }
  }

  @Nested
  class MultiLegPath {

    @BeforeEach
    void setUp() {
      when(routeQueryService.existsDirectRoute(ORIGIN, DEST)).thenReturn(CompletableFuture.completedFuture(false));
      validatorList.clear();
      validatorList.add(validator);
    }

    @Test
    void shouldReturnMultiLegIfIntermediateAirportAndValidSlotsFound() throws ExecutionException, InterruptedException {
      // Arrange
      mockValidMultiLegSetup();
      when(validator.isValidConnection(any(), any())).thenReturn(true);

      // Act
      CompletableFuture<List<Connection>> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      assertEquals(List.of(multiConn), result.get());

      // Verify
      verify(connectionMapper).toMultiLegConnection(ORIGIN, MID, DEST, slotFirstLeg, slotSecondLeg);
    }

    @Test
    void shouldFilterOutMultiLegIfValidatorReturnsFalse() throws ExecutionException, InterruptedException {
      // Arrange
      when(routeQueryService.intermediateAirports(ORIGIN, DEST)).thenReturn(
          CompletableFuture.completedFuture(Set.of(MID)));
      when(scheduleQueryService.findFlightSlots(ORIGIN, MID, SINCE, UNTIL)).thenReturn(
          CompletableFuture.completedFuture(List.of(slotFirstLeg)));
      when(scheduleQueryService.findFlightSlots(MID, DEST, SINCE, UNTIL)).thenReturn(
          CompletableFuture.completedFuture(List.of(slotSecondLeg)));
      when(validator.isValidConnection(any(), any())).thenReturn(false);

      // Act
      CompletableFuture<List<Connection>> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      assertEquals(List.of(), result.get());

      // Verify
      verifyNoInteractions(connectionMapper);
    }

    @Test
    void returnsEmptyWhenNoIntermediateAirports() throws ExecutionException, InterruptedException {
      // Arrange
      when(routeQueryService.intermediateAirports(ORIGIN, DEST)).thenReturn(
          CompletableFuture.completedFuture(Set.of()));

      // Act
      CompletableFuture<List<Connection>> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      assertEquals(List.of(), result.get());

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
    void returnsBothSingleAndMultiLeg() throws ExecutionException, InterruptedException {
      // Arrange
      mockValidSingleLegSetup();
      mockValidMultiLegSetup();
      when(validator.isValidConnection(any(), any())).thenReturn(true);

      // Act
      CompletableFuture<List<Connection>> result = service.findInterconnections(ORIGIN, DEST, SINCE, UNTIL);

      // Assert
      List<Connection> resultList = result.get();

      assertEquals(2, resultList.size());
      assertTrue(resultList.contains(multiConn));
      assertTrue(resultList.contains(directConn));

      // Verify
      verify(connectionMapper).toSingleLegConnection(eq(ORIGIN), eq(DEST), any());
      verify(connectionMapper).toMultiLegConnection(eq(ORIGIN), eq(MID), eq(DEST), any(), any());
    }
  }
}
