package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrometheusClientTest {

    private PrometheusClient client;
    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private HttpResponse<String> mockResponse;

    private final String prometheusUrl = "http://localhost:9090";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Konstruktor, um HttpClient und URL zu injizieren
        client = new PrometheusClient(mockHttpClient, prometheusUrl);
    }

    @Test
    void testIsUp_success_one() throws Exception {
        String jsonResponse = """
                { "status": "success", "data": { "result": [ { "value": [169652, "1"] } ] } }
                """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.<String>send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertTrue(client.isUp("my-target"));
    }

    @Test
    void testIsUp_success_zero() throws Exception {
        String jsonResponse = """
                { "status": "success", "data": { "result": [ { "value": [169652, "0"] } ] } }
                """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("my-target"));
    }

    @Test
    void testIsUp_emptyResult() throws Exception {
        String jsonResponse = """
                { "status": "success", "data": { "result": [] } }
                """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("my-target"));
    }

    @Test
    void testIsUp_invalidJson() throws Exception {
        when(mockResponse.body()).thenReturn("INVALID_JSON");
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("my-target"));
    }

    @Test
    void testIsUp_exceptionDuringHttpCall() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenThrow(new RuntimeException("HTTP fail"));

        assertFalse(client.isUp("my-target"));
    }

    @Test
    void testIsUp_nullOrBlankTarget() {
        assertFalse(client.isUp(null));
        assertFalse(client.isUp(""));
        assertFalse(client.isUp("  "));
    }

    @Test
    void testIsUp_multipleResults_firstSuccess() throws Exception {
        String jsonResponse = """
                {
                  "status": "success",
                  "data": { "result": [ { "value": [169652, "1"] }, { "value": [169653, "0"] } ] }
                }
                """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertTrue(client.isUp("target"));
    }

    @Test
    void testIsUp_multipleResults_firstZero() throws Exception {
        String jsonResponse = """
                {
                  "status": "success",
                  "data": { "result": [ { "value": [169652, "0"] }, { "value": [169653, "1"] } ] }
                }
                """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_urlEncoding() throws Exception {
        String target = "http://my host:1234";
        String encoded = java.net.URLEncoder.encode("probe_success{instance=\"" + target + "\"}", StandardCharsets.UTF_8);

        String jsonResponse = """
                { "status": "success", "data": { "result": [ { "value": [1, "1"] } ] } }
                """;

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenAnswer(invocation -> {
            HttpRequest req = invocation.getArgument(0);
            assertTrue(req.uri().toString().contains(encoded));
            return mockResponse;
        });

        assertTrue(client.isUp(target));
    }
}


