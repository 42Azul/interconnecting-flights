package com.ryanair.interconnector.exception;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ryanair.interconnectingflights.model.Error;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(SpringExtension.class)
class GlobalExceptionHandlerTest {

  @BeforeEach
  void setup() {
    MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new DummyController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    RestAssuredMockMvc.mockMvc(mockMvc);
  }

  @Test
  void shouldReturnBadGatewayForRoutesApiException() {
    Error apiError = given()
        .when()
        .get("/test/routes-exception")
        .then()
        .statusCode(ErrorType.EXTERNAL_API_ERROR.getStatus().value())
        .extract()
        .as(Error.class);

    assertNotNull(apiError);
    assertEquals(ErrorType.EXTERNAL_API_ERROR.getCode(), apiError.getCode());
    assertEquals(ErrorType.EXTERNAL_API_ERROR.getMessage(), apiError.getMessage());
    assertEquals("Error in Routes API", apiError.getDetails());
  }

  @Test
  void shouldReturnBadGatewayForScheduleApiException() {
    Error apiError = given()
        .when()
        .get("/test/schedule-exception/AAA/BBB/2024/6")
        .then()
        .statusCode(ErrorType.EXTERNAL_API_ERROR.getStatus().value())
        .extract()
        .as(Error.class);

    assertNotNull(apiError);
    assertEquals(ErrorType.EXTERNAL_API_ERROR.getCode(), apiError.getCode());
    assertEquals(ErrorType.EXTERNAL_API_ERROR.getMessage(), apiError.getMessage());
    assertEquals("Failed to fetch schedule from AAA to BBB (2024-06)", apiError.getDetails());
  }

  @Test
  void shouldReturnInternalErrorForUnhandledRuntimeException() {
    Error apiError = given()
        .when()
        .get("/test/runtime-exception")
        .then()
        .statusCode(ErrorType.INTERNAL_ERROR.getStatus().value())
        .extract()
        .as(Error.class);

    assertNotNull(apiError);
    assertEquals(ErrorType.INTERNAL_ERROR.getCode(), apiError.getCode());
    assertEquals(ErrorType.INTERNAL_ERROR.getMessage(), apiError.getMessage());
    assertEquals("Unexpected internal server error", apiError.getDetails());
  }

  @Test
  void shouldReturnBadRequestForMissingQueryParam() {
    Error apiError = given()
        .when()
        .get("/test/required-param") // no requiredParam
        .then()
        .statusCode(ErrorType.INVALID_REQUEST.getStatus().value())
        .extract()
        .as(Error.class);

    assertNotNull(apiError);
    assertEquals(ErrorType.INVALID_REQUEST.getCode(), apiError.getCode());
    assertEquals(ErrorType.INVALID_REQUEST.getMessage(), apiError.getMessage());
    assertEquals("Missing required query parameter: 'intParam'", apiError.getDetails());
  }

  @Test
  void shouldReturnBadRequestForTypeMismatch() {
    Error apiError = given()
        .queryParam("intParam", "notANumber")
        .when()
        .get("/test/required-param")
        .then()
        .statusCode(ErrorType.INVALID_REQUEST.getStatus().value())
        .extract()
        .as(Error.class);

    assertNotNull(apiError);
    assertEquals(ErrorType.INVALID_REQUEST.getCode(), apiError.getCode());
    assertEquals(ErrorType.INVALID_REQUEST.getMessage(), apiError.getMessage());
    assertEquals("Invalid value 'notANumber' for parameter 'intParam'. Expected type: int", apiError.getDetails());
  }
}
