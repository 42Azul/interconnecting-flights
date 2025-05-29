package com.ryanair.interconnector.service.impl;

import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnectingflights.model.Leg;
import com.ryanair.interconnector.client.RoutesClient;
import com.ryanair.interconnector.client.SchedulesClient;
import com.ryanair.interconnector.service.InterconnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class InterconnectionServiceImpl implements InterconnectionService {


    private final RoutesClient routesClient;
    private final SchedulesClient schedulesClient;

    @Autowired
    public InterconnectionServiceImpl(RoutesClient routesClient, SchedulesClient schedulesClient) {
        this.routesClient = routesClient;
        this.schedulesClient = schedulesClient;
    }

    // Missing edge case handling for change of month in the range and further improvements/code cleanup

    @Override
    public List<Connection> findInterconnections(String departure, String arrival, LocalDateTime departureDateTime, LocalDateTime arrivalDateTime) {
        return routesClient.fetchAllRoutes().stream().filter(route -> route.getAirportFrom().equalsIgnoreCase(departure) && route.getAirportTo().equalsIgnoreCase(arrival)).filter(route -> route.getOperator() != null).filter(route -> route.getOperator().equalsIgnoreCase("RYANAIR")).flatMap(route -> {
            //TODO Edge case change of month in the range! Not only considering the month of departureDateTime!
            Integer year = departureDateTime.getYear();
            Integer month = departureDateTime.getMonthValue();
            return schedulesClient
                    .getSchedule(route.getAirportFrom(), route.getAirportTo(), year, month).getDays()
                    .stream()
                    .filter(day -> day.getFlights() != null)
                    .flatMap(day ->
                            day
                                    .getFlights()
                                    .stream()
                                    .filter(flight -> departureDateTime.toLocalTime().isBefore(flight.getDepartureTime()) && arrivalDateTime.toLocalTime().isAfter(flight.getArrivalTime()))
                                    .map(flight -> {
                                        LocalDate flightDate = LocalDate.of(year, month, day.getDay());
                                        LocalDateTime offsetArrivalFlight = LocalDateTime.of(flightDate, flight.getArrivalTime());
                                        LocalDateTime offsetDepartureFlight = LocalDateTime.of(flightDate, flight.getDepartureTime());
                                        Leg leg = new Leg().departureAirport(departure).arrivalAirport(arrival).arrivalDateTime(offsetArrivalFlight).departureDateTime(offsetDepartureFlight);
                                        return new Connection().stops(0).legs(List.of(leg));
                                    }));
        }).toList();


    }
}
