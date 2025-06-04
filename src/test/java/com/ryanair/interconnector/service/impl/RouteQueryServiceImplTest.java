package com.ryanair.interconnector.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ryanair.interconnectingflights.external.model.Route;
import com.ryanair.interconnector.client.CachedRoutesProvider;
import com.ryanair.interconnector.testutils.DirectExecutor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class RouteQueryServiceImplTest {

  private static final String VALID_OPERATOR = "RYANAIR";
  private static final String INVALID_OPERATOR = "INVALID";

  private static final String ORIGIN_AIRPORT = "DUB";
  private static final String DESTINATION_AIRPORT = "STN";
  private static final String ORIGIN_AIRPORT_LOWER = "Dub";
  private static final String DESTINATION_AIRPORT_LOWER = "Stn";

  @Mock
  CachedRoutesProvider routesProvider;

  // Using direct execution for the unit tests, as we do not want async checks here
  @Spy
  Executor directExecutor = new DirectExecutor();

  @InjectMocks
  RouteQueryServiceImpl service;

  private final Route validRoute = new Route().airportFrom(ORIGIN_AIRPORT).airportTo(DESTINATION_AIRPORT).operator(
      VALID_OPERATOR);
  private final Route invalidRoute = new Route().airportFrom(ORIGIN_AIRPORT).airportTo(DESTINATION_AIRPORT).operator(INVALID_OPERATOR);

  @Nested
  class ExistsValidRoute {

    @ParameterizedTest
    @CsvSource({
        ORIGIN_AIRPORT + ", " + DESTINATION_AIRPORT,
        ORIGIN_AIRPORT_LOWER + ", " + DESTINATION_AIRPORT_LOWER,
        ORIGIN_AIRPORT + ", " + DESTINATION_AIRPORT_LOWER,
        ORIGIN_AIRPORT_LOWER + ", " + DESTINATION_AIRPORT
    })
    void shouldReturnTrueWhenValidRouteExists(String originAirport, String destinationAirport)
        throws ExecutionException, InterruptedException {
      // Arrange
      when(routesProvider.fetchAllRoutesCached()).thenReturn(List.of(validRoute, invalidRoute));

      // Act
      CompletableFuture<Boolean> result = service.existsDirectRoute(originAirport, destinationAirport);

      // Assert
      assertTrue(result.get());
    }


    @Test
    void shouldReturnFalseIfNoMatchingRouteExistsWithValidOperator() throws ExecutionException, InterruptedException {
      // Arrange
      when(routesProvider.fetchAllRoutesCached()).thenReturn(List.of(invalidRoute));

      // Act
      CompletableFuture<Boolean> result = service.existsDirectRoute(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      assertFalse(result.get());
    }

    @Test
    void shouldReturnFalseIfNoRoutesExist() throws ExecutionException, InterruptedException {
      // Arrange
      when(routesProvider.fetchAllRoutesCached()).thenReturn(List.of());

      // Act
      CompletableFuture<Boolean> result = service.existsDirectRoute(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      assertFalse(result.get());
    }

    @Test
    void shouldReturnFalseIfAllRoutesAreDifferent() throws ExecutionException, InterruptedException {
      // Arrange
      Route differentRoute = new Route().airportFrom(ORIGIN_AIRPORT).airportTo("ABC").operator(VALID_OPERATOR);
      Route anotherDifferentRoute = new Route().airportFrom("LMN").airportTo(DESTINATION_AIRPORT).operator(VALID_OPERATOR);
      when(routesProvider.fetchAllRoutesCached()).thenReturn(List.of(differentRoute, anotherDifferentRoute));

      // Act
      CompletableFuture<Boolean> result = service.existsDirectRoute(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      assertFalse(result.get());
    }
  }

  @Nested
  class IntermediateAirports {

    private static final String INTERMEDIATE_AIRPORT_1 = "STN";
    private static final String INTERMEDIATE_AIRPORT_2 = "BGY";

    private static final Route FROM_ROUTE_1 = new Route().airportFrom(ORIGIN_AIRPORT).airportTo(INTERMEDIATE_AIRPORT_1).operator(
        VALID_OPERATOR);
    private static final Route FROM_ROUTE_2 = new Route().airportFrom(ORIGIN_AIRPORT).airportTo(INTERMEDIATE_AIRPORT_2).operator(
        VALID_OPERATOR);
    private static final Route TO_ROUTE_1 = new Route().airportFrom(INTERMEDIATE_AIRPORT_1).airportTo(DESTINATION_AIRPORT).operator(
        VALID_OPERATOR);
    private static final Route TO_ROUTE_2 = new Route().airportFrom(INTERMEDIATE_AIRPORT_2).airportTo(DESTINATION_AIRPORT).operator(
        VALID_OPERATOR);



    /**
     * Provides various permutations of [FROM_ROUTE_1, FROM_ROUTE_2, TO_ROUTE_1, TO_ROUTE_2] to ensure the method works
     * regardless of the order in which routes are processed.
     */
    static Stream<List<Route>> provideDifferentOrderings() {
      List<Route> baseList = List.of(FROM_ROUTE_1, FROM_ROUTE_2, TO_ROUTE_1, TO_ROUTE_2);
      List<Route> reversed = List.of(TO_ROUTE_2, TO_ROUTE_1, FROM_ROUTE_2, FROM_ROUTE_1);
      List<Route> swappedMid = List.of(FROM_ROUTE_1, TO_ROUTE_2, FROM_ROUTE_2, TO_ROUTE_1);
      List<Route> customOrder = List.of(FROM_ROUTE_2, TO_ROUTE_1, FROM_ROUTE_1, TO_ROUTE_2);

      return Stream.of(baseList, reversed, swappedMid, customOrder);
    }

    @ParameterizedTest
    @MethodSource("provideDifferentOrderings")
    void shouldReturnIntersectionRegardlessOfOrder(List<Route> routesInAnyOrder)
        throws ExecutionException, InterruptedException {
      // Arrange
      when(routesProvider.fetchAllRoutesCached())
          .thenReturn(routesInAnyOrder);

      Set<String> expected = Set.of(INTERMEDIATE_AIRPORT_1, INTERMEDIATE_AIRPORT_2);

      // Act
      CompletableFuture<Set<String>> result = service.intermediateAirports(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      Set<String> resultSet = result.get();
      assertEquals(expected, resultSet);
    }

    @Test
    void shouldReturnIntersectionCaseInsensitive() throws ExecutionException, InterruptedException {
      // Arrange
      when(routesProvider.fetchAllRoutesCached())
          .thenReturn(List.of(FROM_ROUTE_1, TO_ROUTE_1));

      // Act
      CompletableFuture<Set<String>> result = service.intermediateAirports(ORIGIN_AIRPORT_LOWER, DESTINATION_AIRPORT_LOWER);

      // Assert
      Set<String> resultSet = result.get();
      assertEquals(1, resultSet.size());
      assertTrue(resultSet.contains(INTERMEDIATE_AIRPORT_1));
    }

    @Test
    void shouldReturnEmptyIfNoIntersection() throws ExecutionException, InterruptedException {
      // Arrange
      when(routesProvider.fetchAllRoutesCached())
          .thenReturn(List.of(FROM_ROUTE_1, TO_ROUTE_2));

      // Act
      CompletableFuture<Set<String>> result = service.intermediateAirports(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      Set<String> resultSet = result.get();
      assertTrue(resultSet.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(
        value = {
            VALID_OPERATOR + ", " + INVALID_OPERATOR,
            INVALID_OPERATOR + ", " + VALID_OPERATOR,
            INVALID_OPERATOR + ", " + INVALID_OPERATOR
        }
    )
    void shouldIgnoresInvalidOperatorRoutes(String operatorFirstFlight, String operatorSecondFlight)
        throws ExecutionException, InterruptedException {
      // Arrange
      Route wrongOperator1 = new Route().airportFrom(ORIGIN_AIRPORT).airportTo(INTERMEDIATE_AIRPORT_1).operator(operatorFirstFlight);
      Route wrongOperator2 = new Route().airportFrom(INTERMEDIATE_AIRPORT_1).airportTo(DESTINATION_AIRPORT).operator(operatorSecondFlight);

      when(routesProvider.fetchAllRoutesCached()).thenReturn(List.of(wrongOperator1, wrongOperator2));

      // Act
      CompletableFuture<Set<String>> result = service.intermediateAirports(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      Set<String> resultSet = result.get();
      assertTrue(resultSet.isEmpty());
    }
  }
}
