package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.model.ScheduleResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service that acts as a facade for the SchedulesClient, providing a cached version of the schedule response.
 */
@Service
public class CachedSchedulesProvider {

  private final SchedulesClient schedulesClient;

  @Autowired
  public CachedSchedulesProvider(SchedulesClient schedulesClient) {
    this.schedulesClient = schedulesClient;
  }

  @Cacheable(value = "schedulesCache", key = "#departure + #arrival + #year + #month", sync = true)
  public ScheduleResponse getScheduleCached(String departure, String arrival, Integer year, Integer month) {
    return schedulesClient.getSchedule(departure, arrival, year, month);
  }
}
