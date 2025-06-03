package com.ryanair.interconnector;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.ryanair.interconnectingflights.model.Connection;
import com.ryanair.interconnectingflights.model.Error;
import com.ryanair.interconnector.exception.ErrorType;
import com.ryanair.interconnector.testutils.InterconnectionTestScenario;
import io.restassured.common.mapper.TypeRef;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import java.time.YearMonth;
import java.util.List;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Execution(ExecutionMode.SAME_THREAD)
@EnableWireMock({
    @ConfigureWireMock(name = "interconnection")
})
@TestPropertySource(properties = {
    "ryanair.api.routes.url=http://localhost:${wiremock.server.port}",
    "ryanair.api.schedules.url=http://localhost:${wiremock.server.port}"
})
@Slf4j
class InterconnectionControllerIT {

  private static final String API_PATH = "/v1/interconnections";
  private static final String ROUTES_API_PATH = "/views/locate/3/routes";
  private static final String SCHEDULES_API_PATH = "/timtbl/3/schedules/{origin}/{destination}/years/{year}/months/{month}";

  @LocalServerPort
  int port;

  @InjectWireMock("interconnection")
  WireMockServer wireMock;


  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void setup() {
    wireMock.resetToDefaultMappings();
    evictAllCaches(); // Clean between tests, not during
  }

  private void evictAllCaches() {
    cacheManager.getCacheNames().forEach(name -> {
      var cache = cacheManager.getCache(name);
      if (cache != null) {
        cache.clear();
      }
    });
  }

  private List<Connection> callApiWithGivenScenarioOK(InterconnectionTestScenario scenario) {
    return given()
        .port(port)
        .params(scenario.asQueryParams())
        .when()
        .get(API_PATH)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .as(new TypeRef<List<Connection>>() {});
  }

  private Error callApiWithGivenScenarioError(InterconnectionTestScenario scenario, HttpStatusCode expectedStatus) {
    return given()
        .port(port)
        .params(scenario.asQueryParams())
        .when()
        .get(API_PATH)
        .then()
        .statusCode(expectedStatus.value())
        .extract()
        .as(Error.class);
  }

  @ParameterizedTest
  @MethodSource("com.ryanair.interconnector.testutils.InterconnectionTestScenario#withDirectAndStopover")
  void shouldReturnSingleLegAndOneStopConnections(InterconnectionTestScenario scenario) {
    List<Connection> actual = callApiWithGivenScenarioOK(scenario);
    assertEquals(scenario.expectedConnections(), actual);
  }

  @ParameterizedTest
  @MethodSource("com.ryanair.interconnector.testutils.InterconnectionTestScenario#emptyResults")
  void shouldReturnEmptyListForNoMatchingConnections(InterconnectionTestScenario testScenario) {
    List<Connection> actual = callApiWithGivenScenarioOK(testScenario);
    assertTrue(actual.isEmpty());
    actual = callApiWithGivenScenarioOK(testScenario);
    assertTrue(actual.isEmpty());
    // Verify that the Routes API was called only once, ensuring cache works propperly.
    verify(getRequestedFor(urlPathEqualTo(ROUTES_API_PATH)));
    List<ServeEvent> serveEvents = wireMock.getAllServeEvents();
    assertEquals(1, serveEvents.size());
  }

  @ParameterizedTest
  @MethodSource("com.ryanair.interconnector.testutils.InterconnectionTestScenario#invalidRequests")
  void shouldReturnBadRequestForInvalidRequests(InterconnectionTestScenario testScenario) {
    Error error = callApiWithGivenScenarioError(testScenario, HttpStatus.BAD_REQUEST);
    assertNotNull(error);
    assertEquals(ErrorType.INVALID_REQUEST.getCode(), error.getCode());
    assertEquals(ErrorType.INVALID_REQUEST.getMessage(), error.getMessage());

    // Verify that no external API calls were made
    verify(0, anyRequestedFor(anyUrl()));
  }

  @ParameterizedTest
  @MethodSource("com.ryanair.interconnector.testutils.InterconnectionTestScenario#withDirectAndStopover")
  void shouldReturnError500WhenRoutesApiFails(InterconnectionTestScenario scenario) {
    // Arrange (Routes API failure)
    wireMock.resetAll();
    wireMock.stubFor(WireMock.get(urlPathEqualTo(ROUTES_API_PATH))
            .atPriority(1)
        .willReturn(WireMock.aResponse()
            .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .withBody("Internal Server Error")));

    Error error = callApiWithGivenScenarioError(scenario, HttpStatus.BAD_GATEWAY);
    assertNotNull(error);
    assertEquals(ErrorType.EXTERNAL_API_ERROR.getCode(), error.getCode());
    assertEquals(ErrorType.EXTERNAL_API_ERROR.getMessage(), error.getMessage());
    verify(getRequestedFor(urlPathEqualTo(ROUTES_API_PATH)));
    //No more requests to any api should be made since the Routes API failed
    List<ServeEvent> serveEvents = wireMock.getAllServeEvents();
    assertEquals(1, serveEvents.size());
  }

  @ParameterizedTest
  @MethodSource("com.ryanair.interconnector.testutils.InterconnectionTestScenario#withDirectAndStopover")
  void shouldReturnError500WhenSchedulesApiFails(InterconnectionTestScenario scenario) {
    // Arrange (Schedules API failure)
    YearMonth yearMonth = scenario.getDepartureYearMonth();

    wireMock.resetAll();
    wireMock.stubFor(WireMock.get(urlPathTemplate(SCHEDULES_API_PATH))
            .atPriority(1)
            .withPathParam("origin", WireMock.equalTo(scenario.departure()))
            .withPathParam("destination", WireMock.equalTo(scenario.arrival()))
            .withPathParam("year", WireMock.equalTo(String.valueOf(yearMonth.getYear())))
            .withPathParam("month", WireMock.equalTo(String.valueOf(yearMonth.getMonthValue())))
        .willReturn(WireMock.aResponse()
            .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .withBody("Internal Server Error")));

    Error error = callApiWithGivenScenarioError(scenario, HttpStatus.BAD_GATEWAY);
    assertNotNull(error);
    assertEquals(ErrorType.EXTERNAL_API_ERROR.getCode(), error.getCode());
    assertEquals(ErrorType.EXTERNAL_API_ERROR.getMessage(), error.getMessage());
    verify(getRequestedFor(urlPathEqualTo(ROUTES_API_PATH)));
    verify(getRequestedFor(urlPathTemplate(SCHEDULES_API_PATH))
        .withPathParam("origin", WireMock.equalTo(scenario.departure()))
        .withPathParam("destination", WireMock.equalTo(scenario.arrival()))
        .withPathParam("year", WireMock.equalTo(String.valueOf(yearMonth.getYear())))
        .withPathParam("month", WireMock.equalTo(String.valueOf(yearMonth.getMonthValue()))));
  }
}
