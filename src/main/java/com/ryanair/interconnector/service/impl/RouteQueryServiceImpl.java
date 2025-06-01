package com.ryanair.interconnector.service.impl;

import com.ryanair.interconnectingflights.external.model.Route;
import com.ryanair.interconnector.client.RoutesClient;
import com.ryanair.interconnector.service.RouteQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
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

    Mono<Set<String>> reachableFromOrigin = routesClient.fetchAllRoutesAsync()
        .filter(this::isValidRoute)
        .filter(r -> from.equalsIgnoreCase(r.getAirportFrom()))
        .map(Route::getAirportTo)
        .collect(Collectors.toSet());


    Mono<Set<String>> reachableToDestination = routesClient.fetchAllRoutesAsync()
        .filter(this::isValidRoute)
        .filter(r -> to.equalsIgnoreCase(r.getAirportTo()))
        .map(Route::getAirportFrom)
        .collect(Collectors.toSet());

    return Mono.zip(reachableFromOrigin, reachableToDestination)
        .map(tuple -> {
          Set<String> intersection = new HashSet<>(tuple.getT1());
          intersection.retainAll(tuple.getT2());
          return intersection;
        });
  }


  @Override
  public Mono<Boolean> existsDirectRoute(String from, String to) {
    return routesClient.fetchAllRoutesAsync()
        .filter(this::isValidRoute)
        .any(route -> from.equalsIgnoreCase(route.getAirportFrom()) &&
                           to.equalsIgnoreCase(route.getAirportTo()));
  }

  private boolean isValidRoute(Route route){
    return OPERATOR.equals(route.getOperator());
  }

}
