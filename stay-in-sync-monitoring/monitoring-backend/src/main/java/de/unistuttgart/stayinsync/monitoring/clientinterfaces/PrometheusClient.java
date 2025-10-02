package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Client for querying Prometheus to check the availability of monitored targets.
 * <p>
 * This client sends a request to Prometheus' HTTP API to evaluate the
 * {@code probe_success} metric for a given instance (target URL). If the metric
 * returns {@code 1}, the target is considered "up"; otherwise "down".
 */
@ApplicationScoped
public class PrometheusClient {

    private final HttpClient client = HttpClient.newHttpClient();

    @ConfigProperty(name = "prometheus.url")
    String prometheusUrl;

    /**
     * Checks whether a given target URL is up according to Prometheus blackbox probe results.
     *
     * @param targetUrl the monitored target URL
     * @return {@code true} if the target is up, {@code false} otherwise
     */
    public boolean isUp(String targetUrl) {
        try {
            // Build the Prometheus query
            String query = "probe_success{instance=\"" + targetUrl + "\"}";
            URI uri = URI.create(
                    prometheusUrl + "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            Log.info("Prometheus query URL: " + request.uri());

            // Send HTTP request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse JSON response safely using try-with-resources
            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject json = reader.readObject();
                JsonArray result = json.getJsonObject("data").getJsonArray("result");

                if (result.isEmpty()) {
                    // No result means the target is unknown/down
                    return false;
                }

                // Extract probe_success value -> index 1 contains the metric value
                String value = result.getJsonObject(0).getJsonArray("value").getString(1);
                return "1".equals(value);
            }
        } catch (Exception e) {
            Log.error("Failed to query Prometheus for target " + targetUrl, e);
            return false;
        }
    }
}
