package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SyncNodeClient {

    /** HTTP client for requests. */
    private HttpClient client;

    /** ObjectMapper for JSON parsing. */
    private ObjectMapper mapper;

    /** Base URL for SyncNode. */
    @Inject
    @ConfigProperty(name = "syncnode.base.url", defaultValue = "http://localhost:8091")
    String baseUrl;

    /** Default constructor for Quarkus injection. */
    public SyncNodeClient() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Constructor for testing purposes (inject mocks). */
    public SyncNodeClient(HttpClient client, ObjectMapper mapper, String baseUrl) {
        this.client = client;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
    }

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

            return mapper.readValue(response.body(), new TypeReference<List<SnapshotDTO>>() {});
        } catch (Exception e) {
            Log.error("Failed to fetch last five snapshots", e);
            return List.of();
        }
    }

    public SnapshotDTO getById(Long id) {
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
