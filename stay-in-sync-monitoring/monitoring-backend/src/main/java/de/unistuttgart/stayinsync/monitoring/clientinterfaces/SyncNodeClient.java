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
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Client for communicating with the SyncNode service.
 * <p>
 * This class provides methods to fetch monitoring snapshots
 * from the SyncNode backend. It uses Java's built-in {@link HttpClient}
 * to perform HTTP requests and Jackson's {@link ObjectMapper}
 * for JSON serialization/deserialization.
 * <p>
 * The base URL for SyncNode is configurable via MicroProfile Config
 * property {@code syncnode.base.url}, defaulting to
 * {@code http://localhost:8091}.
 */
@ApplicationScoped
public class SyncNodeClient {

    /** HTTP client used for making requests to SyncNode. */
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * ObjectMapper configured to handle Java time types properly and
     * avoid serializing dates as timestamps.
     */
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Base URL for SyncNode, injected from configuration.
     */
    @Inject
    @ConfigProperty(name = "syncnode.base.url", defaultValue = "http://localhost:8091")
    String baseUrl;

    /**
     * Fetches the latest snapshot for all transformations.
     *
     * @return a map where keys are transformation IDs and values are the latest {@link SnapshotDTO},
     *         or an empty map if the request fails or SyncNode returns an error status.
     */
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

            return mapper.readValue(response.body(), new TypeReference<Map<Long, SnapshotDTO>>() {});
        } catch (Exception e) {
            Log.error("Failed to fetch /latestAll from SyncNode", e);
            return Map.of();
        }
    }

    /**
     * Fetches the latest snapshot for a specific transformation.
     *
     * @param transformationId the ID of the transformation
     * @return the latest {@link SnapshotDTO} for the transformation, or {@code null} if unavailable
     */
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

    /**
     * Fetches the last five snapshots for a given transformation.
     *
     * @param transformationId the ID of the transformation
     * @return a list of up to five {@link SnapshotDTO} instances,
     *         or an empty list if the request fails or SyncNode returns an error status
     */
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

            return mapper.readValue(response.body(), new TypeReference<List<SnapshotDTO>>() {});
        } catch (Exception e) {
            Log.error("Failed to fetch last five snapshots", e);
            return List.of();
        }
    }

    /**
     * Fetches a snapshot by its ID.
     *
     * @param id the snapshot ID
     * @return the corresponding {@link SnapshotDTO}, or {@code null} if not found or request fails
     */
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
