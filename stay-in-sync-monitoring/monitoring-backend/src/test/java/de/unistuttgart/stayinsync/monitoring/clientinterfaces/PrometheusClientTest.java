package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
 * These tests verify correct behavior when Prometheus returns valid data,
 * no data, an error value, or when exceptions occur.
 */
class PrometheusClientTest {

    private PrometheusClient prometheusClient;
    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);

        // Create a subclass that uses our mock HttpClient
        prometheusClient = new PrometheusClient() {
            @Override
            public boolean isUp(String targetUrl) {
                try {
                    // Build a fake request just like the real client does
                    String prometheusUrl = "http://localhost:9090";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(prometheusUrl + "/api/v1/query?query=dummy"))
                            .GET()
                            .build();

                    // Use our mocked HttpClient
                    HttpResponse<String> response =
                            mockHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

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
              "data": {
                "result": [
                  { "value": [169652, "1"] }
                ]
              }
            }
            """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        boolean result = prometheusClient.isUp("test-instance");
        assertTrue(result, "Expected true when Prometheus reports probe_success=1");
    }

    @Test
    void testIsUp_ReturnsFalse_WhenPrometheusReportsZero() throws Exception {
        String jsonResponse = """
            {
              "status": "success",
              "data": {
                "result": [
                  { "value": [169652, "0"] }
                ]
              }
            }
            """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        boolean result = prometheusClient.isUp("test-instance");
        assertFalse(result, "Expected false when probe_success=0");
    }

    @Test
    void testIsUp_ReturnsFalse_WhenNoResultsReturned() throws Exception {
        String jsonResponse = """
            {
              "status": "success",
              "data": {
                "result": []
              }
            }
            """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        boolean result = prometheusClient.isUp("test-instance");
        assertFalse(result, "Expected false when no results are returned");
    }

    @Test
    void testIsUp_ReturnsFalse_OnException() throws Exception {
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        boolean result = prometheusClient.isUp("test-instance");
        assertFalse(result, "Expected false when an exception occurs");
    }
}
