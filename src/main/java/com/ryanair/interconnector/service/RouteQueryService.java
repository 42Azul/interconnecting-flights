package com.ryanair.interconnector.service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for querying flight routes.
 * It offers methods to retrieve availability of routes in the form of intermediate airports and to check for direct routes.
 * Uses CompletableFuture for asynchronous operations.
 */

public interface RouteQueryService {

    /**
     * Returns a set of intermediate airports between two given airports.
     * @param from Origin airport code IATA code (e.g. "DUB" for Dublin Airport).
     * @param to Destination airport code IATA code (e.g. "LON" for London airports).
     * @return A CompletableFuture that resolves to a set of IATA codes of intermediate airports in a valid route from 'from' to 'to'.
     */
    CompletableFuture<Set<String>> intermediateAirports(String from, String to);

    /**
     * Checks if a direct route exists between two airports.
     * @param from Origin airport code IATA code (e.g. "DUB" for Dublin Airport).
     * @param to Destination airport code IATA code (e.g. "LON" for London airports).
     * @return A CompletableFuture that resolves to true if a direct route exists, false otherwise.
     */
    CompletableFuture<Boolean> existsDirectRoute(String from, String to);
}
