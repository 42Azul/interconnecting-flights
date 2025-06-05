package com.ryanair.interconnector.validation.route;

import com.ryanair.interconnectingflights.external.model.Route;
import jakarta.validation.constraints.NotNull;

/**
 * Interface for validating a Route depending on different criteria.
 *
 */
public interface RouteValidator {

  boolean isValidRoute(@NotNull Route route);
}
