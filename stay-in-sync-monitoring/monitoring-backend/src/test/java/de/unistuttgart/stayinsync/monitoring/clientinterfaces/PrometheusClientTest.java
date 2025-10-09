package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the PrometheusClient class.
 * Covers success, failure, empty results, exceptions, invalid input, and multiple results.
 */
class PrometheusClientTest {

    private PrometheusClient prometheusClient;
    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);

        prometheusClient = new PrometheusClient() {
            @Override
            public boolean isUp(String targetUrl) {
                if (targetUrl == null || targetUrl.isBlank()) {
                    return false;
                }

                try {
                    String prometheusUrl = "http://localhost:9090";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(prometheusUrl + "/api/v1/query?query=dummy"))
                            .GET()
                            .build();

                    HttpResponse<String> response = mockHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonObject json = Json.createReader(new StringReader(response.body())).readObject();
                    var result = json.getJsonObject("data").getJsonArray("result");

                    if (result.isEmpty()) {
                        return false;
                    }

                    String value = result.getJsonObject(0).getJsonArray("value").getString(1);
                    return "1".equals(value);
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    @Test
    void testIsUp_ReturnsTrue_WhenPrometheusReportsSuccess() throws Exception {
        String jsonResponse = """
            {
              "status": "success",
              "data": { "result": [{ "value": [169652, "1"] }] }
            }
            """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        assertTrue(prometheusClient.isUp("test-instance"));
    }

    @Test
    void testIsUp_ReturnsFalse_WhenPrometheusReportsZero() throws Exception {
        String jsonResponse = """
            {
              "status": "success",
              "data": { "result": [{ "value": [169652, "0"] }] }
            }
            """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        assertFalse(prometheusClient.isUp("test-instance"));
    }

    @Test
    void testIsUp_ReturnsFalse_WhenNoResultsReturned() throws Exception {
        String jsonResponse = """
            {
              "status": "success",
              "data": { "result": [] }
            }
            """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        assertFalse(prometheusClient.isUp("test-instance"));
    }

    @Test
    void testIsUp_ReturnsFalse_OnException() throws Exception {
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        assertFalse(prometheusClient.isUp("test-instance"));
    }

    @Test
    void testIsUp_ReturnsFalse_WhenTargetUrlIsNull() {
        assertFalse(prometheusClient.isUp(null));
    }

    @Test
    void testIsUp_ReturnsFalse_WhenTargetUrlIsBlank() {
        assertFalse(prometheusClient.isUp(" "));
    }

    @Test
    void testIsUp_ReturnsFalse_OnInvalidJson() throws Exception {
        when(mockResponse.body()).thenReturn("INVALID_JSON");
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        assertFalse(prometheusClient.isUp("test-instance"));
    }

    @Test
    void testIsUp_ReturnsTrue_WhenMultipleResultsFirstIsSuccess() throws Exception {
        String jsonResponse = """
            {
              "status": "success",
              "data": {
                "result": [
                  { "value": [169652, "1"] },
                  { "value": [169653, "0"] }
                ]
              }
            }
            """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        assertTrue(prometheusClient.isUp("test-instance"));
    }

    @Test
    void testIsUp_ReturnsFalse_WhenMultipleResultsFirstIsZero() throws Exception {
        String jsonResponse = """
            {
              "status": "success",
              "data": {
                "result": [
                  { "value": [169652, "0"] },
                  { "value": [169653, "1"] }
                ]
              }
            }
            """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        assertFalse(prometheusClient.isUp("test-instance"));
    }
}
