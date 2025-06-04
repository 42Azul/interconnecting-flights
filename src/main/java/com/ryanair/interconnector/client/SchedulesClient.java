package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.api.SchedulesApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for accessing the Ryanair Schedules API from the OpenAPI specification.
 */
@FeignClient(name = "schedulesClient", url = "${ryanair.api.schedules.url}")
public interface SchedulesClient extends SchedulesApi {
}
