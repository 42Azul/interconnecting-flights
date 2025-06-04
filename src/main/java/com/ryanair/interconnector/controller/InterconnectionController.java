package com.ryanair.interconnector.controller;

import com.ryanair.interconnectingflights.api.InterconnectionsApi;
import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnector.service.InterconnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class InterconnectionController implements InterconnectionsApi {

    private final InterconnectionService interconnectionService;

    @Autowired
    public InterconnectionController(InterconnectionService interconnectionService) {
        this.interconnectionService = interconnectionService;
    }

    @Override
    public CompletableFuture<List<Connection>> getInterconnections(String departure, String arrival,
        LocalDateTime departureDateTime, LocalDateTime arrivalDateTime) {

        return interconnectionService.findInterconnections(departure, arrival, departureDateTime, arrivalDateTime);
    }
}
