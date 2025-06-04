package com.ryanair.interconnector.client;

import com.ryanair.interconnectingflights.external.model.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CachedRoutesProvider {

  private final RoutesClient routesClient;

  @Autowired
  public CachedRoutesProvider(RoutesClient routesClient) {
    this.routesClient = routesClient;
  }
  @Cacheable(cacheNames = "routesCache", sync = true)
  public List<Route> fetchAllRoutesCached() {
    return routesClient.fetchAllRoutes();
  }
}
