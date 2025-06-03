package com.ryanair.interconnector.service.impl;

import com.ryanair.interconnectingflights.external.model.Route;
import com.ryanair.interconnector.client.RoutesClient;
import com.ryanair.interconnector.service.RouteQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RouteQueryServiceImpl implements RouteQueryService {

  RoutesClient routesClient;

  private static final String OPERATOR = "RYANAIR";

  @Autowired
  public RouteQueryServiceImpl(RoutesClient routesClient) {
    this.routesClient = routesClient;
  }

  @Override
  public Mono<Set<String>> intermediateAirports(String from, String to) {

    return routesClient.fetchAllRoutesAsync()
        .map(routes -> {
          Set<String> fromOrigin = new HashSet<>();
          Set<String> toDestination = new HashSet<>();

          routes.stream()
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
        }).defaultIfEmpty(Collections.emptySet());
  }

  @Override
  public Mono<Boolean> existsDirectRoute(String from, String to) {
    return routesClient.fetchAllRoutesAsync()
        .map(routes -> routes.stream()
            .filter(this::isValidRoute)
            .anyMatch(route ->
                from.equalsIgnoreCase(route.getAirportFrom()) &&
                    to.equalsIgnoreCase(route.getAirportTo())))
        .defaultIfEmpty(false);
  }

  private boolean isValidRoute(Route route) {
    return OPERATOR.equals(route.getOperator());
  }

}
