package com.ryanair.interconnector.validation.route;

import com.ryanair.interconnectingflights.external.model.Route;
import org.springframework.stereotype.Component;

/**
 * Validates that the connecting airport in a route is null.
 */
@Component
public class NullConnectingAirportValidator implements RouteValidator {
  @Override
  public boolean isValidRoute(Route route) {
    return route.getConnectingAirport() == null;
  }
}
