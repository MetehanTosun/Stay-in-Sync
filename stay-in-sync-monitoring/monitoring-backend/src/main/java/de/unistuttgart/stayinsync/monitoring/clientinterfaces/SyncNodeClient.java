package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Client for communicating with the SyncNode service.
 * Provides methods to fetch monitoring snapshots from the SyncNode backend.
 */
@ApplicationScoped
public class SyncNodeClient {

    /** HTTP client used for making requests to SyncNode. */
    private final HttpClient client;

    /** ObjectMapper configured to handle Java time types properly. */
    private final ObjectMapper mapper;

    /** Base URL for SyncNode, injected from configuration. */
    @Inject
    @ConfigProperty(name = "syncnode.base.url", defaultValue = "http://localhost:8091")
    String baseUrl;

    /**
     * Default constructor used by Quarkus.
     */
    public SyncNodeClient() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Test-only constructor to allow injecting mocks.
     */
    public SyncNodeClient(HttpClient client, ObjectMapper mapper, String baseUrl) {
        this.client = client != null ? client : HttpClient.newHttpClient();
        this.mapper = mapper != null ? mapper
                : new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:8091";
    }

    // === METHODS ===

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

            return mapper.readValue(response.body(), new TypeReference<Map<Long, SnapshotDTO>>() {
            });
        } catch (Exception e) {
            Log.error("Failed to fetch /latestAll from SyncNode", e);
            return Map.of();
        }
    }

    public SnapshotDTO getLatest(Long transformationId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/monitoring/snapshots/latest?transformationId=" + transformationId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Log.error("SyncNode responded with status " + response.statusCode());
                return null;
            }

            return mapper.readValue(response.body(), SnapshotDTO.class);
        } catch (Exception e) {
            Log.error("Failed to fetch latest snapshot", e);
            return null;
        }
    }

    public List<SnapshotDTO> getLastFive(Long transformationId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/monitoring/snapshots/list?transformationId=" + transformationId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Log.error("SyncNode responded with status " + response.statusCode());
                return List.of();
            }

            return mapper.readValue(response.body(), new TypeReference<List<SnapshotDTO>>() {
            });
        } catch (Exception e) {
            Log.error("Failed to fetch last five snapshots", e);
            return List.of();
        }
    }

    // s
    public SnapshotDTO getById(String id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/monitoring/snapshots/" + id))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Log.error("SyncNode responded with status " + response.statusCode());
                return null;
            }

            return mapper.readValue(response.body(), SnapshotDTO.class);
        } catch (Exception e) {
            Log.error("Failed to fetch snapshot with id=" + id, e);
            return null;
        }
    }
}
