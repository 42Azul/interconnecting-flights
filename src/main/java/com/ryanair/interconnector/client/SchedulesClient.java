package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.api.SchedulesApi;
import com.ryanair.interconnectingflights.external.model.ScheduleResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@FeignClient(name = "schedulesClient", url = "${ryanair.api.schedules.url}")
public interface SchedulesClient extends SchedulesApi {

  @Cacheable(value = "schedulesCache", key = "#departure + #arrival + #year + #month")
  default Mono<ScheduleResponse> getScheduleAsync(String departure, String arrival, Integer year, Integer month){

    return Mono.fromCallable(() -> getSchedule(departure, arrival, year, month)).subscribeOn(Schedulers.boundedElastic());
  }

}
