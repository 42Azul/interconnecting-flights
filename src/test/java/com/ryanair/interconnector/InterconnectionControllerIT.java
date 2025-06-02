package com.ryanair.interconnector;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnector.testutils.InterconnectionTestScenario;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.EnableWireMock;
import java.util.List;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@EnableWireMock
@TestPropertySource(properties = {
    "ryanair.api.routes.url=http://localhost:${wiremock.server.port}",
    "ryanair.api.schedules.url=http://localhost:${wiremock.server.port}"
})
class InterconnectionControllerIT {

  private static final String API_PATH = "/v1/interconnections";

  @LocalServerPort
  int port;

  private List<Connection> callApiWithGivenScenario(InterconnectionTestScenario scenario,
      HttpStatusCode expectedStatus) {
    ValidatableResponse response =  given()
        .port(port)
        .params(scenario.asQueryParams())
        .when()
        .get(API_PATH)
        .then()
        .statusCode(expectedStatus.value());

    if(expectedStatus.is2xxSuccessful()){
      return response
          .extract()
          .as(new TypeRef<List<Connection>>() {
          });
    }
    return List.of();
  }

  @ParameterizedTest
  @MethodSource("com.ryanair.interconnector.testutils.InterconnectionTestScenario#withDirectAndStopover")
  void shouldReturnSingleLegAndOneStopConnections(InterconnectionTestScenario scenario) {
    List<Connection> actual = callApiWithGivenScenario(scenario, HttpStatus.OK);
    assertEquals(scenario.expectedConnections(), actual);
  }

  @ParameterizedTest
  @MethodSource("com.ryanair.interconnector.testutils.InterconnectionTestScenario#emptyResults")
  void shouldReturnEmptyListForNoMatchingConnections(InterconnectionTestScenario testScenario) {
    List<Connection> actual = callApiWithGivenScenario(testScenario, HttpStatus.OK);
    assertTrue(actual.isEmpty());
  }

  @ParameterizedTest
  @MethodSource("com.ryanair.interconnector.testutils.InterconnectionTestScenario#invalidRequests")
  void shouldReturnBadRequestForInvalidRequests(InterconnectionTestScenario testScenario) {
    callApiWithGivenScenario(testScenario, HttpStatus.BAD_REQUEST);
  }
}
