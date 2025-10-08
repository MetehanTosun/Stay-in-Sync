package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import de.unistuttgart.stayinsync.monitoring.service.LogService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class LogResourceTest {

    @InjectMock
    LogService logService;

    @Test
    void getLogs_shouldReturnLogs() {
        LogEntryDto log = new LogEntryDto(null,"INFO" , "test-log", null, null);

        when(logService.fetchAndParseLogs(null, 1000L, 2000L, "INFO"))
                .thenReturn(List.of(log));

        given()
                .queryParam("startTime", 1000)
                .queryParam("endTime", 2000)
                .queryParam("level", "INFO")
                .when()
                .get("/api/logs")
                .then()
                .statusCode(200)
                .body("[0].message", equalTo("test-log"))
                .body("[0].level", equalTo("INFO"));

        verify(logService).fetchAndParseLogs(null, 1000L, 2000L, "INFO");
    }

    @Test
    void getLogsByTransformationIds_shouldReturnLogs() {
        LogEntryDto log = new LogEntryDto(null, "ERROR", "tf-log", null, null);

        when(logService.fetchAndParseLogsForTransformations(List.of("tf1", "tf2"), 5000L, 6000L, "ERROR"))
                .thenReturn(List.of(log));

        given()
                .contentType("application/json")
                .body(List.of("tf1", "tf2"))
                .queryParam("startTime", 5000)
                .queryParam("endTime", 6000)
                .queryParam("level", "ERROR")
                .when()
                .post("/api/logs/transformations")
                .then()
                .statusCode(200)
                .body("[0].message", equalTo("tf-log"))
                .body("[0].level", equalTo("ERROR"));

        verify(logService).fetchAndParseLogsForTransformations(List.of("tf1", "tf2"), 5000L, 6000L, "ERROR");
    }

    @Test
    void getLogsByService_shouldReturnLogs() {
        LogEntryDto log = new LogEntryDto(null, "DEBUG", "service-log", null, null);

        when(logService.fetchAndParseLogsForService("my-service", 0L, 9999L, "DEBUG"))
                .thenReturn(List.of(log));

        given()
                .queryParam("service", "my-service")
                .queryParam("startTime", 0)
                .queryParam("endTime", 9999)
                .queryParam("level", "DEBUG")
                .when()
                .get("/api/logs/service")
                .then()
                .statusCode(200)
                .body("[0].message", equalTo("service-log"))
                .body("[0].level", equalTo("DEBUG"));

        verify(logService).fetchAndParseLogsForService("my-service", 0L, 9999L, "DEBUG");
    }
}
