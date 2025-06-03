package com.ryanair.interconnector.service;

import reactor.core.publisher.Mono;
import java.util.Set;

public interface RouteQueryService {

    Mono<Set<String>> intermediateAirports(String from, String to);
    Mono<Boolean> existsDirectRoute(String from, String to);
}
