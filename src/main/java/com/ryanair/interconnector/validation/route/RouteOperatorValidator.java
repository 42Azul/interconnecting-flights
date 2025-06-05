package com.ryanair.interconnector.validation.route;

import com.ryanair.interconnectingflights.external.model.Route;
import org.springframework.stereotype.Component;

/**
 * Validates that the route is operated by Ryanair.
 */
@Component
public class RouteOperatorValidator  implements  RouteValidator {

  private static final String OPERATOR = "RYANAIR";

  @Override
  public boolean isValidRoute(Route route) {
    return OPERATOR.equals(route.getOperator());
  }
}
