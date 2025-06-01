package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.api.RoutesApi;
import com.ryanair.interconnectingflights.external.model.Route;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@FeignClient(name = "routesClient", url = "${ryanair.api.routes.url}")
public interface RoutesClient extends RoutesApi {

  @Cacheable(value = "routesCache")
  default Flux<Route> fetchAllRoutesAsync(){
    return Flux.fromIterable(fetchAllRoutes()).subscribeOn(Schedulers.boundedElastic());
  }
}
