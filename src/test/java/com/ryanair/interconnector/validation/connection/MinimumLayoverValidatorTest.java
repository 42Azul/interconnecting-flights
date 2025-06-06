package com.ryanair.interconnector.validation.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ryanair.interconnector.dto.FlightSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Stream;

class MinimumLayoverValidatorTest {

  // Constants for the first flight slot
  private static final LocalDateTime DEPARTURE = LocalDateTime.of(2025, 6, 1, 10, 0);
  private static final LocalDateTime ARRIVAL = DEPARTURE.plusHours(1);

  @Nested
  class ValidInput {

    @ParameterizedTest
    @CsvSource({
        "50, 51, true",
        "40, 40, true",
        "30, 29, false"
    })
    void shouldValidateLayover(int minimumLayoverTime, int flightGapInMinutes, boolean expected) {
      // Arrange
      FlightSlot first = new FlightSlot(DEPARTURE, ARRIVAL);
      LocalDateTime secondDep = ARRIVAL.plusMinutes(flightGapInMinutes);
      FlightSlot second = new FlightSlot(secondDep, secondDep.plusHours(2));
      MinimumLayoverValidator validator =
          new MinimumLayoverValidator(Duration.ofMinutes(minimumLayoverTime));

      // Act
      boolean result = validator.isValidConnection(first, second);

      // Assert
      assertEquals(expected, result);
    }
  }

  @Nested
  class InvalidInput {

    private MinimumLayoverValidator validator;

    @BeforeEach
    void setUp() {
      validator = new MinimumLayoverValidator(Duration.ofHours(0));
    }

    static Stream<Arguments> invalidCombinations() {
      FlightSlot ok = new FlightSlot(DEPARTURE, ARRIVAL);
      FlightSlot noDep = new FlightSlot(null, ARRIVAL);
      FlightSlot noArr = new FlightSlot(DEPARTURE, null);

      return Stream.of(
          Arguments.of(null, ok),
          Arguments.of(ok, null),
          Arguments.of(null, null),
          Arguments.of(noArr, ok),
          Arguments.of(noDep, ok),
          Arguments.of(ok, noDep),
          Arguments.of(ok, noArr),
          Arguments.of(noArr, noDep)
      );
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    void shouldReturnFalseForAnyInvalidInput(FlightSlot first, FlightSlot second) {
      boolean result = validator.isValidConnection(first, second);
      assertFalse(result);
    }
  }
}
