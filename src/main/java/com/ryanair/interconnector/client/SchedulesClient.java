package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.api.SchedulesApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "schedulesClient", url = "${ryanair.api.routes.url}")
public interface SchedulesClient extends SchedulesApi {

}
