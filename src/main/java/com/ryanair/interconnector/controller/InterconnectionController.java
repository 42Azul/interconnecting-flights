package com.ryanair.interconnector.controller;


import com.ryanair.interconnectingflights.api.InterconnectionsApi;
import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnector.service.InterconnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
public class InterconnectionController implements InterconnectionsApi {

    private final InterconnectionService interconnectionService;

    @Autowired
    public InterconnectionController(InterconnectionService interconnectionService) {
        this.interconnectionService = interconnectionService;
    }

    @Override
    public ResponseEntity<List<Connection>> getInterconnections(String departure, String arrival, LocalDateTime departureDateTime, LocalDateTime arrivalDateTime) {
        List<Connection> connections = interconnectionService.findInterconnections(departure, arrival, departureDateTime, arrivalDateTime);
        return ResponseEntity.ok(connections);
    }
}
