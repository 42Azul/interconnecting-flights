package com.ryanair.interconnector.service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface RouteQueryService {

    CompletableFuture<Set<String>> intermediateAirports(String from, String to);
    CompletableFuture<Boolean> existsDirectRoute(String from, String to);
}
