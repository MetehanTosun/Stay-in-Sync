package de.unistuttgart.stayinsync.monitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class LogServiceTest {

    private LogService service;

    void setupMock(String body, int status) throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        service = new LogService(new ObjectMapper(), httpClient);

        when(httpResponse.statusCode()).thenReturn(status);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
    }

    static Stream<TestCase> logMethodsProvider() {
        return Stream.of(
                new TestCase("fetchAndParseLogs", "42", 0, 10, "INFO"),
                new TestCase("fetchAndParseLogsForTransformations", List.of("T1"), 0, 10, "WARN"),
                new TestCase("fetchAndParseLogsForService", "svcX", 0, 10, "DEBUG")
        );
    }

    @ParameterizedTest
    @MethodSource("logMethodsProvider")
    void testLogsMethods_orderedByTimestamp(TestCase testCase) throws Exception {
        // JSON mit zwei Logs unterschiedlicher Timestamps (200 > 100)
        String body = """
        {
          "data": {
            "result": [
              {
                "stream": { "service": "svcX", "level": "DEBUG", "transformationId":"T1" },
                "values": [
                  ["100", "{\\"message\\":\\"Message1\\", \\"syncJobId\\":\\"42\\"}"],
                  ["200", "Raw log entry"]
                ]
              }
            ]
          }
        }
        """;

        setupMock(body, 200);

        List<LogEntryDto> logs;
        switch (testCase.method) {
            case "fetchAndParseLogs" -> logs = service.fetchAndParseLogs((String)testCase.arg1,
                    testCase.start, testCase.end, testCase.level);
            case "fetchAndParseLogsForTransformations" -> logs = service.fetchAndParseLogsForTransformations(
                    (List<String>)testCase.arg1, testCase.start, testCase.end, testCase.level);
            case "fetchAndParseLogsForService" -> logs = service.fetchAndParseLogsForService(
                    (String)testCase.arg1, testCase.start, testCase.end, testCase.level);
            default -> throw new IllegalArgumentException("Unknown method");
        }

        assertThat(logs).hasSize(2);

        // Prüfen, dass Logs nach Timestamp absteigend sortiert sind
        assertThat(logs.get(0).timestamp()).isEqualTo("200");
        assertThat(logs.get(0).message()).isEqualTo("Raw log entry");

        assertThat(logs.get(1).timestamp()).isEqualTo("100");
        assertThat(logs.get(1).message()).isEqualTo("Message1");
    }

    @ParameterizedTest
    @MethodSource("logMethodsProvider")
    void testLogsMethods_shouldThrowOnHttpError(TestCase testCase) throws Exception {
        setupMock("error", 500);

        // Create a single ThrowingCallable that calls the right method
        org.assertj.core.api.ThrowableAssert.ThrowingCallable call = () -> {
            switch (testCase.method) {
                case "fetchAndParseLogs" -> service.fetchAndParseLogs((String) testCase.arg1,
                        testCase.start, testCase.end, testCase.level);
                case "fetchAndParseLogsForTransformations" -> service.fetchAndParseLogsForTransformations(
                        (List<String>) testCase.arg1, testCase.start, testCase.end, testCase.level);
                case "fetchAndParseLogsForService" -> service.fetchAndParseLogsForService(
                        (String) testCase.arg1, testCase.start, testCase.end, testCase.level);
                default -> throw new IllegalArgumentException("Unknown method: " + testCase.method);
            }
        };

        assertThatThrownBy(call)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error fetching or parsing logs");
    }


    static class TestCase {
        String method;
        Object arg1;
        long start;
        long end;
        String level;

        TestCase(String method, Object arg1, long start, long end, String level) {
            this.method = method;
            this.arg1 = arg1;
            this.start = start;
            this.end = end;
            this.level = level;
        }
    }
}
