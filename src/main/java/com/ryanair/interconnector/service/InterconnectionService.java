package com.ryanair.interconnector.service;

import com.ryanair.interconnectingflights.model.Connection;

import java.time.LocalDateTime;
import java.util.List;

public interface InterconnectionService {

    List<Connection> findInterconnections(String departure, String arrival, LocalDateTime departureDateTime, LocalDateTime arrivalDateTime);

}
