package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.api.RoutesApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "routesClient", url = "${ryanair.api.routes.url}")
public interface RoutesClient extends RoutesApi {
}
