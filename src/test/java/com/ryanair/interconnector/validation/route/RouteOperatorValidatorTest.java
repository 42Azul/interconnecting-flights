package com.ryanair.interconnector.validation.route;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ryanair.interconnectingflights.external.model.Route;
import org.junit.jupiter.api.Test;

class RouteOperatorValidatorTest {

  RouteValidator routeValidator = new RouteOperatorValidator();

  @Test
  void testIsValidRoute() {
    //Arrange
    Route validRoute = new Route();
    validRoute.setOperator("RYANAIR");

    // Act & Assert
    assertTrue(routeValidator.isValidRoute(validRoute));
  }

  @Test
  void testIsInvalidRoute() {
    // Arrange
    Route invalidRoute = new Route();
    invalidRoute.setOperator("OTHER_AIRLINE");

    // Act & Assert
    assertFalse(routeValidator.isValidRoute(invalidRoute));
  }

}