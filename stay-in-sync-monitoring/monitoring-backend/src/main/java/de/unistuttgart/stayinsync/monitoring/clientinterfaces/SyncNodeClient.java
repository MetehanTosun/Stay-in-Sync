package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;


import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Client for interacting with the SyncNode API to fetch snapshot data.
 * This client uses Java HttpClient for making requests and Jackson for JSON deserialization.
 */
@ApplicationScoped
public class SyncNodeClient {

    private static final String BASE_URL = "http://localhost:8091";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SyncNodeClient() {
        this.httpClient = HttpClient.newHttpClient();

        // Configure ObjectMapper to handle Java 8 date/time types
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule()) // ensures proper Java Time support
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // use ISO-8601 format
    }

    /**
     * Fetches the latest snapshots for all monitored entities from the SyncNode.
     *
     * @return a Map where the key is the entity ID and the value is the latest SnapshotDTO
     */
    public Map<Long, SnapshotDTO> getLatestAll() {
        try {
            // Build GET request for the /monitoring/snapshots/latestAll endpoint
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/monitoring/snapshots/latestAll"))
                    .GET()
                    .build();

            // Send the request and get the response as a String
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check for non-success HTTP status
            if (response.statusCode() != 200) {
                Log.error("SyncNode responded with status " + response.statusCode());
                return Map.of(); // return empty map on failure
            }

            // Deserialize JSON response into Map<Long, SnapshotDTO>
            return objectMapper.readValue(response.body(), new TypeReference<Map<Long, SnapshotDTO>>() {});

        } catch (Exception e) {
            Log.error("Failed to fetch /latestAll from SyncNode", e);
            return Map.of(); // return empty map on exception
        }
    }
}


