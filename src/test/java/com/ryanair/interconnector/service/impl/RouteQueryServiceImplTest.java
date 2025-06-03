package com.ryanair.interconnector.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ryanair.interconnectingflights.external.model.Route;
import com.ryanair.interconnector.client.RoutesClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.List;
import java.util.Set;
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
  RoutesClient routesClient;

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
    void shouldReturnTrueWhenValidRouteExists(String originAirport, String destinationAirport) {
      // Arrange
      when(routesClient.fetchAllRoutesAsync()).thenReturn(Mono.just(List.of(validRoute, invalidRoute)));

      // Act
      Mono<Boolean> result = service.existsDirectRoute(originAirport, destinationAirport);

      // Assert
      StepVerifier.create(result)
          .expectNext(true)
          .verifyComplete();
    }


    @Test
    void shouldReturnFalseIfNoMatchingRouteExistsWithValidOperator() {
      // Arrange
      when(routesClient.fetchAllRoutesAsync()).thenReturn(Mono.just(List.of(invalidRoute)));

      // Act
      Mono<Boolean> result = service.existsDirectRoute(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      StepVerifier.create(result)
          .expectNext(false)
          .verifyComplete();
    }

    @Test
    void shouldReturnFalseIfNoRoutesExist() {
      // Arrange
      when(routesClient.fetchAllRoutesAsync()).thenReturn(Mono.empty());

      // Act
      Mono<Boolean> result = service.existsDirectRoute(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      StepVerifier.create(result)
          .expectNext(false)
          .verifyComplete();
    }

    @Test
    void shouldReturnFalseIfAllRoutesAreDifferent() {
      // Arrange
      Route differentRoute = new Route().airportFrom(ORIGIN_AIRPORT).airportTo("ABC").operator(VALID_OPERATOR);
      Route anotherDifferentRoute = new Route().airportFrom("LMN").airportTo(DESTINATION_AIRPORT).operator(VALID_OPERATOR);
      when(routesClient.fetchAllRoutesAsync()).thenReturn(Mono.just(List.of(differentRoute, anotherDifferentRoute)));

      // Act
      Mono<Boolean> result = service.existsDirectRoute(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      StepVerifier.create(result)
          .expectNext(false)
          .verifyComplete();
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
    void shouldReturnIntersectionRegardlessOfFluxOrder(List<Route> routesInAnyOrder) {
      // Arrange
      when(routesClient.fetchAllRoutesAsync())
          .thenReturn(Mono.just(routesInAnyOrder));

      Set<String> expected = Set.of(INTERMEDIATE_AIRPORT_1, INTERMEDIATE_AIRPORT_2);

      // Act
      Mono<Set<String>> result = service.intermediateAirports(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(actualSet -> actualSet.equals(expected))
          .verifyComplete();
    }

    @Test
    void shouldReturnIntersectionCaseInsensitive() {
      // Arrange
      when(routesClient.fetchAllRoutesAsync())
          .thenReturn(Mono.just(List.of(FROM_ROUTE_1, TO_ROUTE_1)));

      // Act
      Mono<Set<String>> result = service.intermediateAirports(ORIGIN_AIRPORT_LOWER, DESTINATION_AIRPORT_LOWER);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(set -> set.contains(INTERMEDIATE_AIRPORT_1) && set.size() == 1)
          .verifyComplete();
    }

    @Test
    void shouldReturnEmptyIfNoIntersection() {
      // Arrange
      when(routesClient.fetchAllRoutesAsync())
          .thenReturn(Mono.just(List.of(FROM_ROUTE_1, TO_ROUTE_2)));

      // Act
      Mono<Set<String>> result = service.intermediateAirports(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(Set::isEmpty)
          .verifyComplete();
    }

    @ParameterizedTest
    @CsvSource(
        value = {
            VALID_OPERATOR + ", " + INVALID_OPERATOR,
            INVALID_OPERATOR + ", " + VALID_OPERATOR,
            INVALID_OPERATOR + ", " + INVALID_OPERATOR
        }
    )
    void shouldIgnoresInvalidOperatorRoutes(String operatorFirstFlight, String operatorSecondFlight) {
      // Arrange
      Route wrongOperator1 = new Route().airportFrom(ORIGIN_AIRPORT).airportTo(INTERMEDIATE_AIRPORT_1).operator(operatorFirstFlight);
      Route wrongOperator2 = new Route().airportFrom(INTERMEDIATE_AIRPORT_1).airportTo(DESTINATION_AIRPORT).operator(operatorSecondFlight);

      when(routesClient.fetchAllRoutesAsync()).thenReturn(Mono.just(List.of(wrongOperator1, wrongOperator2)));

      // Act
      Mono<Set<String>> result = service.intermediateAirports(ORIGIN_AIRPORT, DESTINATION_AIRPORT);

      // Assert
      StepVerifier.create(result)
          .expectNextMatches(Set::isEmpty)
          .verifyComplete();
    }
  }
}
