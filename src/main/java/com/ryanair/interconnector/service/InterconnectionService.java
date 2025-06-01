package com.ryanair.interconnector.service;

import com.ryanair.interconnectingflights.model.Connection;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;

public interface InterconnectionService {

  Flux<Connection> findInterconnections(String departure, String arrival, LocalDateTime departureDateTime,
      LocalDateTime arrivalDateTime);

}
