package com.ryanair.interconnector.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnectingflights.model.Leg;
import com.ryanair.interconnector.dto.FlightSlot;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class ConnectionMapperTest {

  private final ConnectionMapper mapper = new ConnectionMapper();

  private static final String ORIGIN = "DUB";
  private static final String MID = "STN";
  private static final String DEST = "WRO";

  private static final FlightSlot VALID_SLOT = new FlightSlot(
      LocalDateTime.of(2023, 1, 1, 10, 0),
      LocalDateTime.of(2023, 1, 1, 12, 0)
  );

  @Nested
  class ToSingleLegConnection {

    static List<FlightSlot> invalidSlots() {
      return List.of(
          new FlightSlot(null, null),
          new FlightSlot(LocalDateTime.of(2023, 1, 1, 10, 0), null),
          new FlightSlot(null, LocalDateTime.of(2023, 1, 1, 12, 0))
      );
    }

    @Test
    void shouldMapCorrectly() {
      // Act
      Optional<Connection> result = mapper.toSingleLegConnection(ORIGIN, DEST, VALID_SLOT);

      // Assert
      assertTrue(result.isPresent());
      Connection connection = result.get();
      assertEquals(0, connection.getStops());
      assertEquals(1, connection.getLegs().size());

      Leg leg = connection.getLegs().get(0);
      assertEquals(ORIGIN, leg.getDepartureAirport());
      assertEquals(DEST, leg.getArrivalAirport());
      assertEquals(VALID_SLOT.departureDateTime(), leg.getDepartureDateTime());
      assertEquals(VALID_SLOT.arrivalDateTime(), leg.getArrivalDateTime());
    }

    @Test
    void shouldReturnEmptyWhenSlotIsNull() {
      // Act & Assert
      assertTrue(mapper.toSingleLegConnection(ORIGIN, DEST, null).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("invalidSlots")
    void shouldReturnEmptyWhenSlotFieldsAreNull(FlightSlot slot) {
      // Act & Assert
      assertTrue(mapper.toSingleLegConnection(ORIGIN, DEST, slot).isEmpty());
    }


    @Test
    void shouldReturnEmptyWhenDepartureOrArrivalCodeIsNull() {
      // Act & Assert
      assertTrue(mapper.toSingleLegConnection(null, DEST, VALID_SLOT).isEmpty());
      assertTrue(mapper.toSingleLegConnection(ORIGIN, null, VALID_SLOT).isEmpty());
    }

  }

  @Nested
  class ToMultiLegConnection {

    private static final FlightSlot SECOND_SLOT = new FlightSlot(
        LocalDateTime.of(2023, 1, 1, 14, 0),
        LocalDateTime.of(2023, 1, 1, 16, 0)
    );

    @Test
    void shouldMapCorrectly() {
      // Act
      Optional<Connection> result = mapper.toMultiLegConnection(ORIGIN, MID, DEST, VALID_SLOT, SECOND_SLOT);

      // Assert
      assertTrue(result.isPresent());
      Connection connection = result.get();
      assertEquals(1, connection.getStops());
      assertEquals(2, connection.getLegs().size());

      Leg leg1 = connection.getLegs().get(0);
      Leg leg2 = connection.getLegs().get(1);

      assertEquals(ORIGIN, leg1.getDepartureAirport());
      assertEquals(MID, leg1.getArrivalAirport());
      assertEquals(MID, leg2.getDepartureAirport());
      assertEquals(DEST, leg2.getArrivalAirport());
    }

    @Test
    void shouldReturnEmptyIfAnyLegIsInvalid() {
      // Arrange
      FlightSlot invalid = new FlightSlot(null, null);

      // Act & Assert
      assertTrue(mapper.toMultiLegConnection(ORIGIN, MID, DEST, VALID_SLOT, invalid).isEmpty());
      assertTrue(mapper.toMultiLegConnection(ORIGIN, MID, DEST, invalid, VALID_SLOT).isEmpty());
    }

    @Test
    void shouldReturnEmptyIfAnyAirportIsNull() {
      assertTrue(mapper.toMultiLegConnection(null, MID, DEST, VALID_SLOT, SECOND_SLOT).isEmpty());
      assertTrue(mapper.toMultiLegConnection(ORIGIN, null, DEST, VALID_SLOT, SECOND_SLOT).isEmpty());
      assertTrue(mapper.toMultiLegConnection(ORIGIN, MID, null, VALID_SLOT, SECOND_SLOT).isEmpty());
    }
  }
}
