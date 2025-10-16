package de.unistuttgart.stayinsync.monitoring.clientinterfaces;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.net.URLEncoder;
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

        // Create PrometheusClient with injected HttpClient and URL for testing
        client = new PrometheusClient(mockHttpClient, prometheusUrl);
    }

    @Test
    void testIsUp_success_one() throws Exception {
        // Test that isUp returns true when Prometheus reports 1
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
        // Test that isUp returns false when Prometheus reports 0
        String jsonResponse = """
                { "status": "success", "data": { "result": [ { "value": [169652, "0"] } ] } }
                """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("my-target"));
    }

    @Test
    void testIsUp_emptyResult() throws Exception {
        // Test that isUp returns false when Prometheus returns no results
        String jsonResponse = """
                { "status": "success", "data": { "result": [] } }
                """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("my-target"));
    }

    @Test
    void testIsUp_invalidJson() throws Exception {
        // Test that isUp returns false when the response is invalid JSON
        when(mockResponse.body()).thenReturn("INVALID_JSON");
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("my-target"));
    }

    @Test
    void testIsUp_exceptionDuringHttpCall() throws Exception {
        // Test that isUp returns false if an exception occurs during HTTP call
        when(mockHttpClient.<String>send(any(), any())).thenThrow(new RuntimeException("HTTP fail"));

        assertFalse(client.isUp("my-target"));
    }

    @Test
    void testIsUp_nullOrBlankTarget() {
        // Test that isUp returns false for null or blank target URLs
        assertFalse(client.isUp(null));
        assertFalse(client.isUp(""));
        assertFalse(client.isUp("  "));
    }

    @Test
    void testIsUp_multipleResults_firstSuccess() throws Exception {
        // Test multiple results, first is 1
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
        // Test multiple results, first is 0
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
        // Test that the target URL is correctly encoded in the request
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
    @Test
    void testIsUp_multipleResults_allZero() throws Exception {
        // Test multiple results, all 0
        String jsonResponse = """
            { "status": "success", "data": { "result": [ { "value": [1, "0"] }, { "value": [2, "0"] } ] } }
            """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_multipleResults_mixedSecondSuccess() throws Exception {
        // Test multiple results, second is 1 but first is 0 (should return false)
        String jsonResponse = """
            { "status": "success", "data": { "result": [ { "value": [1, "0"] }, { "value": [2, "1"] } ] } }
            """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_emptyPrometheusUrl() throws Exception {
        // Temporarily set prometheusUrl to empty
        client.prometheusUrl = "";
        String jsonResponse = """
            { "status": "success", "data": { "result": [ { "value": [1, "1"] } ] } }
            """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("target")); // URI.create("") will throw exception internally
    }

    @Test
    void testIsUp_exceptionParsingJson() throws Exception {
        // Test JSON that lacks "data" field
        String jsonResponse = """
            { "status": "success" }
            """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_exceptionDuringJsonReaderClose() throws Exception {
        // Mock HttpResponse.body() to throw an exception when creating JsonReader
        HttpResponse<String> badResponse = mock(HttpResponse.class);
        when(badResponse.body()).thenThrow(new RuntimeException("Cannot read body"));
        when(mockHttpClient.<String>send(any(), any())).thenReturn(badResponse);

        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_longTargetUrl() throws Exception {
        // Test very long target URL
        String target = "http://example.com/" + "a".repeat(1000);
        String jsonResponse = """
            { "status": "success", "data": { "result": [ { "value": [1, "1"] } ] } }
            """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertTrue(client.isUp(target));
    }

    @Test
    void testIsUp_unicodeTargetUrl() throws Exception {
        // Test target URL containing Unicode characters
        String target = "http://exämple.com/тест";
        String jsonResponse = """
            { "status": "success", "data": { "result": [ { "value": [1, "1"] } ] } }
            """;
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertTrue(client.isUp(target));
    }
    @Test
    void testDefaultConstructorNotNull() {
        // Cover the default constructor path
        PrometheusClient defaultClient = new PrometheusClient();
        assertNotNull(defaultClient);
    }

    @Test
    void testIsUp_httpResponseStatusNon200() throws Exception {
        // When HTTP response code is not 200, isUp should return false
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("{ \"status\": \"success\", \"data\": { \"result\": [] } }");
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_nullBody() throws Exception {
        // When response body is null
        when(mockResponse.body()).thenReturn(null);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_emptyStringBody() throws Exception {
        // When response body is empty string
        when(mockResponse.body()).thenReturn("");
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_largeNumberOfResults() throws Exception {
        // Simulate a large array of results
        StringBuilder sb = new StringBuilder("{ \"status\": \"success\", \"data\": { \"result\": [");
        for (int i = 0; i < 100; i++) {
            sb.append("{ \"value\": [").append(i).append(", \"0\"] },");
        }
        sb.append("{ \"value\": [101, \"1\"] } ] } }"); // last one is success
        String jsonResponse = sb.toString();

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);

        // First element is 0, but the client only checks the first result, should return false
        assertFalse(client.isUp("target"));
    }

    @Test
    void testIsUp_exceptionOnUriCreate() {
        // Passing an invalid URI will throw exception inside isUp
        String invalidTarget = "http://\0invalid-url";
        assertFalse(client.isUp(invalidTarget));
    }
    @Test
    void testIsUp_executesFullPathSuccessOne() throws Exception {
        // Real HttpRequest path, body returns 1
        String target = "real-target";
        String jsonResponse = "{ \"status\": \"success\", \"data\": { \"result\": [ { \"value\": [123, \"1\"] } ] } }";

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.<String>send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest req = invocation.getArgument(0);
                    // Assert URI is correctly built
                    assertTrue(req.uri().toString().contains(URLEncoder.encode("probe_success{instance=\"" + target + "\"}", StandardCharsets.UTF_8)));
                    return mockResponse;
                });

        assertTrue(client.isUp(target));
    }

    @Test
    void testIsUp_executesFullPathSuccessZero() throws Exception {
        String target = "real-target";
        String jsonResponse = "{ \"status\": \"success\", \"data\": { \"result\": [ { \"value\": [123, \"0\"] } ] } }";

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertFalse(client.isUp(target));
    }

    @Test
    void testIsUp_emptyResultExecutesFullPath() throws Exception {
        String target = "empty-result";
        String jsonResponse = "{ \"status\": \"success\", \"data\": { \"result\": [] } }";

        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.<String>send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertFalse(client.isUp(target));
    }

    @Test
    void testIsUp_invalidJsonExecutesFullPath() throws Exception {
        String target = "invalid-json";

        when(mockResponse.body()).thenReturn("NOT_JSON");
        when(mockHttpClient.<String>send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertFalse(client.isUp(target));
    }

    @Test
    void testIsUp_httpSendThrowsExceptionExecutesFullPath() throws Exception {
        String target = "http-fail";

        when(mockHttpClient.<String>send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("HTTP failed"));

        assertFalse(client.isUp(target));
    }

    @Test
    void testIsUp_uriEncodeException() {
        // Invalid target string that causes URI.create to throw
        String invalidTarget = "http://\0bad";

        assertFalse(client.isUp(invalidTarget));
    }

}



