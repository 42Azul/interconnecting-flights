package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.api.RoutesApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for accessing the Routes API of Ryanair from the OpenAPI specification.
 */
@FeignClient(name = "routesClient", url = "${ryanair.api.routes.url}")
public interface RoutesClient extends RoutesApi {
}
