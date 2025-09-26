package de.unistuttgart.stayinsync.monitoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Service class responsible for fetching and parsing log entries
 * from a Loki server via its HTTP API.
 *
 * This service converts Loki's JSON response into structured {@link LogEntryDto} objects,
 * which can then be used by other components in the system.
 */
@ApplicationScoped
public class LogService {

    // Base URL of the Loki query_range API endpoint
    private static final String LOKI_URL = "http://localhost:3100/loki/api/v1/query_range";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Default constructor: initializes a standard HttpClient and ObjectMapper.
     */
    public LogService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Alternative constructor, mainly for unit testing.
     * Allows injecting mock {@link ObjectMapper} and {@link HttpClient}.
     */
    public LogService(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    /**
     * Fetches logs from Loki filtered by a syncJobId (if provided) and log level.
     *
     * @param syncJobId The sync job identifier (optional).
     * @param startNs   Start time in nanoseconds.
     * @param endNs     End time in nanoseconds.
     * @param level     Log level filter (e.g., INFO, ERROR) (optional).
     * @return A list of {@link LogEntryDto} parsed from Loki logs.
     */
    public List<LogEntryDto> fetchAndParseLogs(String syncJobId, long startNs, long endNs, String level) {
        try {
            // Build Loki label selectors
            List<String> labels = new ArrayList<>();
            if (syncJobId != null && !syncJobId.isBlank()) {
                labels.add("syncJobId=\"" + syncJobId + "\"");
            } else {
                labels.add("agent=\"fluent-bit\"");
            }
            if (level != null && !level.isBlank()) {
                labels.add("level=\"" + level.toUpperCase() + "\"");
            }

            // Query with label selector
            String query = "{" + String.join(",", labels) + "}";

            // Build Loki API request URL
            String url = String.format("%s?query=%s&start=%d&end=%d&limit=5000&direction=backward",
                    LOKI_URL,
                    URLEncoder.encode(query, StandardCharsets.UTF_8),
                    startNs,
                    endNs);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            Log.info("Request send: " + request);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Ensure request was successful
            if (response.statusCode() != 200) {
                throw new RuntimeException("Loki call failed: " + response.statusCode() + " - " + response.body());
            }

            // Parse JSON response
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("data").path("result");

            List<LogEntryDto> logs = new ArrayList<>();
            for (JsonNode stream : result) {
                String service = stream.path("stream").path("service").asText(null);
                String lvl     = stream.path("stream").path("level").asText(null);

                for (JsonNode value : stream.path("values")) {
                    String timestamp = value.get(0).asText();
                    String messageJson = value.get(1).asText();
                    String message;
                    String parsedSyncJobId = null;

                    try {
                        // Parse outer JSON from Loki value
                        JsonNode messageNode = objectMapper.readTree(messageJson);

                        // Extract message field if available
                        message = messageNode.path("message").asText(messageJson);

                        // Extract syncJobId if present
                        parsedSyncJobId = messageNode.path("syncJobId").asText(null);

                        // Handle case where message contains nested JSON
                        if (message.startsWith("{") && message.endsWith("}")) {
                            try {
                                JsonNode innerNode = objectMapper.readTree(message);
                                if (innerNode.has("syncJobId")) {
                                    parsedSyncJobId = innerNode.get("syncJobId").asText();
                                }
                                message = innerNode.path("message").asText(message);
                            } catch (Exception ignore) {
                                // Not valid inner JSON → ignore
                            }
                        }
                    } catch (Exception ex) {
                        // If value is not JSON → use raw string
                        message = messageJson;
                    }

                    logs.add(new LogEntryDto(service, lvl, message, timestamp, parsedSyncJobId));
                }
            }

            return logs;

        } catch (Exception e) {
            Log.error("Error fetching logs", e);
            throw new RuntimeException("Error fetching or parsing logs", e);
        }
    }

    /**
     * Fetch logs for multiple transformationIds using Loki.
     *
     * @param transformationIds List of transformation IDs to query for.
     * @param startNs           Start time in nanoseconds.
     * @param endNs             End time in nanoseconds.
     * @param level             Log level filter (optional).
     * @return A list of {@link LogEntryDto} for the given transformationIds.
     */
    public List<LogEntryDto> fetchAndParseLogsForTransformations(List<String> transformationIds, long startNs, long endNs, String level) {
        try {
            if (transformationIds == null || transformationIds.isEmpty()) {
                return new ArrayList<>();
            }

            // Loki label filters
            List<String> labels = new ArrayList<>();
            labels.add("agent=\"fluent-bit\"");

            // Add log level filter if provided
            if (level != null && !level.isBlank()) {
                labels.add("level=\"" + level.toUpperCase() + "\"");
            }

            // Add transformationIds using regex match
            String regex = String.join("|", transformationIds);
            labels.add("transformationId=~\"" + regex + "\"");

            // Build Loki query
            String query = "{" + String.join(",", labels) + "}";

            String url = String.format("%s?query=%s&start=%d&end=%d&limit=5000&direction=backward",
                    LOKI_URL,
                    URLEncoder.encode(query, StandardCharsets.UTF_8),
                    startNs,
                    endNs);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            Log.info("Generated URL: " + url);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Loki call failed: " + response.statusCode() + " - " + response.body());
            }

            // Parse JSON response
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("data").path("result");

            List<LogEntryDto> logs = new ArrayList<>();
            for (JsonNode stream : result) {
                String service = stream.path("stream").path("service").asText(null);
                String lvl     = stream.path("stream").path("level").asText(null);
                String transId = stream.path("stream").path("transformationId").asText(null);

                for (JsonNode value : stream.path("values")) {
                    String timestamp = value.get(0).asText();
                    String messageJson = value.get(1).asText();
                    String message;
                    try {
                        JsonNode messageNode = objectMapper.readTree(messageJson);
                        message = messageNode.path("message").asText(messageJson);
                    } catch (Exception e) {
                        message = messageJson;
                    }
                    logs.add(new LogEntryDto(service, lvl, message, timestamp, transId));
                }
            }

            // Sort logs by timestamp ascending
            logs.sort(Comparator.comparingLong(a -> Long.parseLong(a.timestamp())));

            return logs;
        } catch (Exception e) {
            Log.error("Error fetching logs for transformationIds", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetch logs for a specific service.
     *
     * @param service The service name.
     * @param startNs Start time in nanoseconds.
     * @param endNs   End time in nanoseconds.
     * @param level   Log level filter (optional).
     * @return A list of {@link LogEntryDto} for the given service.
     */
    public List<LogEntryDto> fetchAndParseLogsForService(String service, long startNs, long endNs, String level) {
        try {
            if (service == null || service.isBlank()) {
                return new ArrayList<>();
            }

            // Loki label filters
            List<String> labels = new ArrayList<>();
            labels.add("agent=\"fluent-bit\"");
            labels.add("service=\"" + service + "\"");

            // Add log level filter if provided
            if (level != null && !level.isBlank()) {
                labels.add("level=\"" + level.toUpperCase() + "\"");
            }

            // Build Loki query
            String query = "{" + String.join(",", labels) + "}";

            String url = String.format("%s?query=%s&start=%d&end=%d&limit=5000&direction=backward",
                    LOKI_URL,
                    URLEncoder.encode(query, StandardCharsets.UTF_8),
                    startNs,
                    endNs);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            Log.info("Generated URL for service logs: " + url);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Loki call failed: " + response.statusCode() + " - " + response.body());
            }

            // Parse JSON response
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("data").path("result");

            List<LogEntryDto> logs = new ArrayList<>();
            for (JsonNode stream : result) {
                String srv  = stream.path("stream").path("service").asText(null);
                String lvl  = stream.path("stream").path("level").asText(null);

                for (JsonNode value : stream.path("values")) {
                    String timestamp = value.get(0).asText();
                    String messageJson = value.get(1).asText();
                    String message;
                    try {
                        JsonNode messageNode = objectMapper.readTree(messageJson);
                        message = messageNode.path("message").asText(messageJson);
                    } catch (Exception e) {
                        message = messageJson;
                    }
                    logs.add(new LogEntryDto(srv, lvl, message, timestamp, null));
                }
            }

            // Sort logs by timestamp ascending
            logs.sort(Comparator.comparingLong(a -> Long.parseLong(a.timestamp())));
            return logs;

        } catch (Exception e) {
            Log.error("Error fetching logs for service", e);
            throw new RuntimeException(e);
        }
    }
}

