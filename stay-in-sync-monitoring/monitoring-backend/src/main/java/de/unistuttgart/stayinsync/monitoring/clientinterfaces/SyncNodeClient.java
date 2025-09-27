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

@ApplicationScoped
public class SyncNodeClient {

    private final HttpClient client = HttpClient.newHttpClient();

    // Hier ObjectMapper mit JavaTimeModule registrieren
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())      // <- wichtig
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // optional, ISO-Format

    private final String baseUrl = "http://localhost:8091"; // SyncNode

    public Map<Long, SnapshotDTO> getLatestAll() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/monitoring/snapshots/latestAll"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                Log.error("SyncNode responded with status " + response.statusCode());
                return Map.of();
            }

            // JSON in Map<Long, SnapshotDTO> umwandeln
            return mapper.readValue(response.body(), new TypeReference<Map<Long, SnapshotDTO>>() {});
        } catch (Exception e) {
            Log.error("Failed to fetch /latestAll from SyncNode", e);
            return Map.of();
        }
    }
}


