import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import de.unistuttgart.stayinsync.monitoring.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/*
class LogServiceTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private LogService service;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);

        // Service mit Dependency Injection bauen
        service = new LogService(new ObjectMapper(), httpClient);
    }


        @Test
        void fetchAndParseLogs_shouldReturnLogs_whenValidResponse() throws Exception {
            String body = """
            {
              "data": {
                "result": [
                  {
                    "stream": { "service": "svcA", "level": "INFO" },
                    "values": [
                      ["123", "{\\"message\\":\\"Hello world\\", \\"syncJobId\\":\\"42\\"}"]
                    ]
                  }
                ]
              }
            }
            """;

            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn(body);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            List<LogEntryDto> logs = service.fetchAndParseLogs("42", 0L, 10L, "info");

            assertThat(logs).hasSize(1);
            assertThat(logs.getFirst().service()).isEqualTo("svcA");
            assertThat(logs.getFirst().level()).isEqualTo("INFO");
            assertThat(logs.getFirst().message()).isEqualTo("Hello world");
        }

        @Test
        void fetchAndParseLogs_shouldThrow_whenHttpError() throws Exception {
            when(httpResponse.statusCode()).thenReturn(500);
            when(httpResponse.body()).thenReturn("server error");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            assertThatThrownBy(() -> service.fetchAndParseLogs("42", 0, 10, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error fetching or parsing logs");
        }
        /*
            @Test
            void fetchAndParseLogs_shouldHandleNonJsonMessage() throws Exception {
                String body = """
                {
                  "data": {
                    "result": [
                      {
                        "stream": { "service": "svcB", "level": "ERROR" },
                        "values": [
                          ["999", "raw-text-log-entry"]
                        ]
                      }
                    ]
                  }
                }
                """;

                when(httpResponse.statusCode()).thenReturn(200);
                when(httpResponse.body()).thenReturn(body);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(httpResponse);

                List<LogEntryDto> logs = service.fetchAndParseLogs(null, 0, 10, "error");

                assertThat(logs).hasSize(1);
                assertThat(logs.getFirst().message()).isEqualTo("raw-text-log-entry");
                assertThat(logs.getFirst().level()).isEqualTo("ERROR");
            }

            @Test
            void fetchAndParseLogsForTransformations_shouldReturnSortedLogs() throws Exception {
                String body = """
                {
                  "data": {
                    "result": [
                      {
                        "stream": { "service": "svcC", "level": "WARN", "transformationId":"T1" },
                        "values": [
                          ["200", "{\\"message\\":\\"B\\"}"],
                          ["100", "{\\"message\\":\\"A\\"}"]
                        ]
                      }
                    ]
                  }
                }
                """;

                when(httpResponse.statusCode()).thenReturn(200);
                when(httpResponse.body()).thenReturn(body);
                when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                        .thenReturn(httpResponse);

                List<LogEntryDto> logs = service.fetchAndParseLogsForTransformations(List.of("T1"), 0, 10, null);

                assertThat(logs).extracting(LogEntryDto::message)
                        .containsExactly("B", "A");
            }
       
    @Test
    void fetchAndParseLogsForTransformations_shouldReturnEmpty_whenNoIds() {
        List<LogEntryDto> logs = service.fetchAndParseLogsForTransformations(List.of(), 0, 10, "INFO");
        assertThat(logs).isEmpty();
        verifyNoInteractions(httpClient);
    }
}
        */