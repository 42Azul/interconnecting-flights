package com.ryanair.interconnector.testutils;

import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnectingflights.model.Leg;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Accessors(chain = true, fluent = true)
@AllArgsConstructor
public class InterconnectionTestScenario {

  private static final String DUB = "DUB";
  private static final String STN = "STN";
  private static final String WRO = "WRO";
  private static final String ALC = "ALC";
  private static final String MRS = "MRS";

  private static final String DEPARTURE_DATE_ISO = "2023-06-01T00:00";
  private static final String ARRIVAL_DATE_ISO = "2023-06-01T23:59";
  private static final String DEPARTURE_DATE_INVALID = "2023-06-01";
  private static final String ARRIVAL_DATE_INVALID = "2023-06-01";

  private final String departure;
  private final String arrival;
  private final String departureDateTime;
  private final String arrivalDateTime;
  private final List<Connection> expectedConnections;


  public Map<String, String> asQueryParams() {
    return Stream.of(
            Optional.ofNullable(departure).map(v -> Map.entry("departure", v)),
            Optional.ofNullable(arrival).map(v -> Map.entry("arrival", v)),
            Optional.ofNullable(departureDateTime).map(v -> Map.entry("departureDateTime", v)),
            Optional.ofNullable(arrivalDateTime).map(v -> Map.entry("arrivalDateTime", v))
        )
        .flatMap(Optional::stream)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public YearMonth getDepartureYearMonth() {
    return YearMonth.from(LocalDateTime.parse(departureDateTime));
  }


  public static Stream<InterconnectionTestScenario> withDirectAndStopover() {
    return Stream.of(new InterconnectionTestScenario(
        DUB, WRO,
        DEPARTURE_DATE_ISO, ARRIVAL_DATE_ISO,
        List.of(
            // Direct flight
            new Connection().stops(0).legs(List.of(
                new Leg()
                    .departureAirport(DUB)
                    .arrivalAirport(WRO)
                    .departureDateTime(LocalDateTime.of(2023, 6, 1, 18, 0))
                    .arrivalDateTime(LocalDateTime.of(2023, 6, 1, 21, 35))
            )),
            // Interconnected flight (1 stop)
            new Connection().stops(1).legs(List.of(
                new Leg()
                    .departureAirport(DUB)
                    .arrivalAirport(STN)
                    .departureDateTime(LocalDateTime.of(2023, 6, 1, 6, 25))
                    .arrivalDateTime(LocalDateTime.of(2023, 6, 1, 7, 35)),
                new Leg()
                    .departureAirport(STN)
                    .arrivalAirport(WRO)
                    .departureDateTime(LocalDateTime.of(2023, 6, 1, 9, 50))
                    .arrivalDateTime(LocalDateTime.of(2023, 6, 1, 13, 20))
            ))
        )
    ));
  }

  public static Stream<InterconnectionTestScenario> emptyResults() {
    return Stream.of(new InterconnectionTestScenario(
        ALC, MRS,
        DEPARTURE_DATE_ISO, ARRIVAL_DATE_ISO,
        List.of()
    ),
        new InterconnectionTestScenario(
            DUB, DUB,
            DEPARTURE_DATE_ISO, ARRIVAL_DATE_ISO,
            List.of()
        )
    );
  }

  public static Stream<InterconnectionTestScenario> invalidRequests() {
    return Stream.of(
        new InterconnectionTestScenario(null, WRO, DEPARTURE_DATE_ISO, ARRIVAL_DATE_ISO, null),
        new InterconnectionTestScenario(DUB, null, DEPARTURE_DATE_ISO, ARRIVAL_DATE_ISO, null),
        new InterconnectionTestScenario(DUB, WRO, DEPARTURE_DATE_INVALID, ARRIVAL_DATE_ISO, null),
        new InterconnectionTestScenario(DUB, WRO, DEPARTURE_DATE_ISO, ARRIVAL_DATE_INVALID, null),
        new InterconnectionTestScenario(DUB, WRO, null, null, null)
    );
  }
}
