package com.ryanair.interconnector.validation.route;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ryanair.interconnectingflights.external.model.Route;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NullConnectingAirportValidatorTest {

  RouteValidator routeValidator = new NullConnectingAirportValidator();

  @Test
  void testValidRoute() {
    // Arrange
    Route route = new Route();
    route.setConnectingAirport(null);

    // Act & Assert
    assertTrue(routeValidator.isValidRoute(route));
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = { "DUB", "DUUUB", "  " })
  void testInvalidRoute(String nonNullConnectingAirport) {
    // Arrange
    Route route = new Route();
    route.setConnectingAirport(nonNullConnectingAirport);

    // Act & Assert
    assertFalse(routeValidator.isValidRoute(route));
  }

}