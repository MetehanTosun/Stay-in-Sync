package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class PrometheusClient {
    private final HttpClient client = HttpClient.newHttpClient();


    @ConfigProperty(name = "prometheus.url")
    String prometheusUrl;

    public boolean isUp(String targetUrl) {
        try {
            String query = "probe_success{instance=\"" + targetUrl + "\"}";
            URI uri = URI.create(prometheusUrl + "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();


            Log.info("Prometheus Url: " + request.uri());

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject json = Json.createReader(new StringReader(response.body())).readObject();
            JsonArray result = json.getJsonObject("data").getJsonArray("result");

            if (result.isEmpty()) {
                return false; // keine Daten -> unknown/down
            }

            String value = result.getJsonObject(0).getJsonArray("value").getString(1);
            return "1".equals(value);
        } catch (Exception e) {
            return false;
        }
    }
}

