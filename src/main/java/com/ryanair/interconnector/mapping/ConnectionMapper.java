package com.ryanair.interconnector.mapping;

import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnectingflights.model.Leg;
import com.ryanair.interconnector.dto.FlightSlot;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class ConnectionMapper {

    public Optional<Connection> toSingleLegConnection(String from, String to, FlightSlot slot) {
        return toLeg(from, to, slot)
            .map(leg -> new Connection()
                .stops(0)
                .legs(List.of(leg))
            );
    }

    public Optional<Connection> toMultiLegConnection(
        String from, String via, String to,
        FlightSlot first, FlightSlot second
    ) {
        Optional<Leg> leg1 = toLeg(from, via, first);
        Optional<Leg> leg2 = toLeg(via, to, second);

        if(leg1.isEmpty() || leg2.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Connection()
            .stops(1)
            .legs(List.of(leg1.get(), leg2.get()))
        );
    }

    private Optional<Leg> toLeg(String dep, String arr, FlightSlot slot) {
        if (dep == null || arr == null || slot == null || slot.departureDateTime() == null || slot.arrivalDateTime() == null) {
            return Optional.empty();
        }

        return Optional.of(new Leg()
            .departureAirport(dep)
            .arrivalAirport(arr)
            .departureDateTime(slot.departureDateTime())
            .arrivalDateTime(slot.arrivalDateTime())
        );
    }
}
