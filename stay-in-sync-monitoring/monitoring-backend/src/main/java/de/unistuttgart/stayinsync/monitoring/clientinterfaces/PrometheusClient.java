package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Client to interact with Prometheus server and check metric values.
 */
@ApplicationScoped
public class PrometheusClient {

    private static final String PROMETHEUS_BASE_URL = "http://localhost:9090";
    private final HttpClient httpClient;

    public PrometheusClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Checks if the target instance is up based on the Prometheus 'probe_success' metric.
     *
     * @param targetInstanceUrl the target instance URL
     * @return true if the target instance is up, false otherwise
     */
    public boolean isUp(String targetInstanceUrl) {
        try {
            // Build Prometheus query for the given instance
            String query = buildProbeSuccessQuery(targetInstanceUrl);

            // Build HTTP GET request to Prometheus API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROMETHEUS_BASE_URL + "/api/v1/query?query=" + query))
                    .GET()
                    .build();

            Log.info("Sending Prometheus request to URL: " + request.uri());

            // Send the request and get response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse JSON response
            JsonObject jsonResponse = Json.createReader(new StringReader(response.body())).readObject();
            JsonArray resultArray = jsonResponse.getJsonObject("data").getJsonArray("result");

            // If no data is returned, consider the target down
            if (resultArray.isEmpty()) {
                return false;
            }

            String value = resultArray.getJsonObject(0)
                    .getJsonArray("value")
                    .getString(1);
            return "1".equals(value);

        } catch (Exception e) {
            Log.error("Failed to query Prometheus for instance " + targetInstanceUrl, e);
            return false;
        }
    }

    /**
     * Helper method to build URL-encoded Prometheus query for 'probe_success' metric.
     *
     * @param targetInstanceUrl the target instance URL
     * @return URL-encoded Prometheus query string
     */
    private String buildProbeSuccessQuery(String targetInstanceUrl) {
        String rawQuery = String.format("probe_success{instance=\"%s\"}", targetInstanceUrl);
        return URLEncoder.encode(rawQuery, StandardCharsets.UTF_8);
    }
}

