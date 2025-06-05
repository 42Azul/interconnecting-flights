package com.ryanair.interconnector.service.impl;

import com.ryanair.interconnectingflights.external.model.Route;
import com.ryanair.interconnector.client.CachedRoutesProvider;
import com.ryanair.interconnector.service.RouteQueryService;
import com.ryanair.interconnector.validation.route.RouteValidator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Service implementation for querying flight routes.
 * This implementation filter routes by the different validators and uses the cached routes provider
 * for the asynchronous operations.
 */

@Service
public class RouteQueryServiceImpl implements RouteQueryService {


  private final CachedRoutesProvider routesProvider;
  private final Executor externalApiExecutor;
  private final List<RouteValidator> routeValidators;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "List is injected by Spring and not exposed"
  )
  @Autowired
  public RouteQueryServiceImpl(CachedRoutesProvider routesProvider, @Qualifier("externalApiExecutor") Executor externalApiExecutor,
                               List<RouteValidator> routeValidators) {
    this.routesProvider = routesProvider;
    this.externalApiExecutor = externalApiExecutor;
    this.routeValidators = routeValidators;
  }

  @Override
  public CompletableFuture<Set<String>> intermediateAirports(String from, String to) {
    return CompletableFuture.supplyAsync(() -> {
      Set<String> fromOrigin = new HashSet<>();
      Set<String> toDestination = new HashSet<>();

      routesProvider.fetchAllRoutesCached().stream()
          .filter(this::isValidRoute)
          .forEach(route -> {
            if (from.equalsIgnoreCase(route.getAirportFrom())) {
              fromOrigin.add(route.getAirportTo());
            }
            if (to.equalsIgnoreCase(route.getAirportTo())) {
              toDestination.add(route.getAirportFrom());
            }
          });

      fromOrigin.retainAll(toDestination);
      return fromOrigin;
    }, externalApiExecutor);
  }

  @Override
  public CompletableFuture<Boolean> existsDirectRoute(String from, String to) {
    return CompletableFuture.supplyAsync(() -> routesProvider.fetchAllRoutesCached().stream()
        .filter(this::isValidRoute)
        .anyMatch(route ->
            from.equalsIgnoreCase(route.getAirportFrom()) &&
                to.equalsIgnoreCase(route.getAirportTo())), externalApiExecutor);
  }

  private boolean isValidRoute(Route route) {
    return routeValidators.stream().allMatch(validator -> validator.isValidRoute(route));
  }

}
