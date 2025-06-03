package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.api.RoutesApi;
import com.ryanair.interconnectingflights.external.model.Route;
import com.ryanair.interconnector.exception.RoutesApiException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.List;

@FeignClient(name = "routesClient", url = "${ryanair.api.routes.url}")
public interface RoutesClient extends RoutesApi {

  // Workaround to be able to cache in sync mode
  @Cacheable(
      cacheNames = "routesCache"
  )
  default List<Route> fetchAllRoutesCached() {
    return fetchAllRoutes();
  }


  default Mono<List<Route>> fetchAllRoutesAsync(){
    return Mono.fromCallable(this::fetchAllRoutesCached)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(ex -> new RoutesApiException("Failed to fetch routes", ex));
  }
}
