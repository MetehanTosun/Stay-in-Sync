package de.unistuttgart.stayinsync.monitoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ApplicationScoped
public class LogService {

    // === Konstanten ===
    private static final String AGENT_LABEL = "agent=\"fluent-bit\"";
    private static final String SERVICE_LABEL = "service=\"%s\"";
    private static final String SYNCJOB_LABEL = "syncJobId=\"%s\"";
    private static final String TRANSFORMATION_LABEL = "transformationId=~\"%s\"";
    private static final String LEVEL_LABEL = "level=\"%s\"";

    private static final String QUERY_TEMPLATE =
            "%s?query=%s&start=%d&end=%d&limit=5000&direction=backward";

    private static final String JSON_FIELD_DATA = "data";
    private static final String JSON_FIELD_RESULT = "result";
    private static final String JSON_FIELD_STREAM = "stream";
    private static final String JSON_FIELD_VALUES = "values";
    private static final String JSON_FIELD_MESSAGE = "message";
    private static final String JSON_FIELD_LEVEL = "level";
    private static final String JSON_FIELD_SERVICE = "service";
    private static final String JSON_FIELD_SYNCJOB = "syncJobId";
    private static final String JSON_FIELD_TRANSFORMATION = "transformationId";

    @ConfigProperty(name = "loki.url")
    String LOKI_URL;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LogService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // Für Unit-Tests
    public LogService(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.LOKI_URL = "http://localhost:3100";
    }

    // === Public APIs ===

    public List<LogEntryDto> fetchAndParseLogs(String syncJobId, long startNs, long endNs, String level) {
        List<String> labels = new ArrayList<>();
        labels.add(syncJobId != null && !syncJobId.isBlank()
                ? String.format(SYNCJOB_LABEL, syncJobId)
                : AGENT_LABEL);

        addLevelLabel(labels, level);

        String query = buildQuery(labels);
        return fetchLogs(query, startNs, endNs, true);
    }

    public List<LogEntryDto> fetchAndParseLogsForTransformations(List<String> transformationIds,
                                                                 long startNs, long endNs, String level) {
        if (transformationIds == null || transformationIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> labels = new ArrayList<>(List.of(AGENT_LABEL));
        addLevelLabel(labels, level);

        String regex = String.join("|", transformationIds);
        labels.add(String.format(TRANSFORMATION_LABEL, regex));

        String query = buildQuery(labels);
        return fetchLogs(query, startNs, endNs, false);
    }

    public List<LogEntryDto> fetchAndParseLogsForService(String service,
                                                         long startNs, long endNs, String level) {
        if (service == null || service.isBlank()) {
            return new ArrayList<>();
        }

        List<String> labels = new ArrayList<>(List.of(AGENT_LABEL, String.format(SERVICE_LABEL, service)));
        addLevelLabel(labels, level);

        String query = buildQuery(labels);
        return fetchLogs(query, startNs, endNs, false);
    }

    // === Private Helpers ===

    private void addLevelLabel(List<String> labels, String level) {
        if (level != null && !level.isBlank()) {
            labels.add(String.format(LEVEL_LABEL, level.toUpperCase()));
        }
    }

    private String buildQuery(List<String> labels) {
        return "{" + String.join(",", labels) + "}";
    }

    private List<LogEntryDto> fetchLogs(String query, long startNs, long endNs, boolean parseSyncJob) {
        try {
            String url = String.format(QUERY_TEMPLATE,
                    LOKI_URL,
                    URLEncoder.encode(query, StandardCharsets.UTF_8),
                    startNs, endNs);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            Log.info("Sending Loki request: " + request);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Log.error("Loki request failed with status code " + response.statusCode());
            }

            JsonNode result = objectMapper.readTree(response.body())
                    .path(JSON_FIELD_DATA)
                    .path(JSON_FIELD_RESULT);

            List<LogEntryDto> logs = new ArrayList<>();
            for (JsonNode stream : result) {
                String service = stream.path(JSON_FIELD_STREAM).path(JSON_FIELD_SERVICE).asText(null);
                String lvl = stream.path(JSON_FIELD_STREAM).path(JSON_FIELD_LEVEL).asText(null);
                String transId = stream.path(JSON_FIELD_STREAM).path(JSON_FIELD_TRANSFORMATION).asText(null);

                for (JsonNode value : stream.path(JSON_FIELD_VALUES)) {
                    String timestamp = value.get(0).asText();
                    String rawMessage = value.get(1).asText();

                    String parsedMessage;
                    String parsedSyncJobId = null;

                    if (parseSyncJob) {
                        ParsedMessage parsed = parseMessageWithSyncJob(rawMessage);
                        parsedMessage = parsed.message();
                        parsedSyncJobId = parsed.syncJobId();
                    } else {
                        parsedMessage = parseMessage(rawMessage);
                    }

                    logs.add(new LogEntryDto(service, lvl, parsedMessage, timestamp,
                            parseSyncJob ? parsedSyncJobId : transId));
                }
            }

            logs.sort(Comparator.comparingLong((LogEntryDto a) -> Long.parseLong(a.timestamp())).reversed());
            return logs;

        } catch (Exception e) {
            Log.error("Error fetching logs", e);
            throw new RuntimeException("Error fetching or parsing logs", e);
        }
    }

    private String parseMessage(String messageJson) {
        try {
            JsonNode messageNode = objectMapper.readTree(messageJson);
            return messageNode.path(JSON_FIELD_MESSAGE).asText(messageJson);
        } catch (Exception e) {
            Log.error("Error parsing message", e);
            return messageJson;
        }
    }

    private ParsedMessage parseMessageWithSyncJob(String messageJson) {
        try {
            JsonNode messageNode = objectMapper.readTree(messageJson);
            String message = messageNode.path(JSON_FIELD_MESSAGE).asText(messageJson);
            String syncJobId = messageNode.path(JSON_FIELD_SYNCJOB).asText(null);

            if (message.startsWith("{") && message.endsWith("}")) {
                try {
                    JsonNode inner = objectMapper.readTree(message);
                    if (inner.has(JSON_FIELD_SYNCJOB)) {
                        syncJobId = inner.get(JSON_FIELD_SYNCJOB).asText();
                    }
                    message = inner.path(JSON_FIELD_MESSAGE).asText(message);
                } catch (Exception ignore) {
                    // ignore invalid inner JSON
                }
            }
            return new ParsedMessage(message, syncJobId);
        } catch (Exception e) {
            Log.error("Error parsing message", e);
            return new ParsedMessage(messageJson, null);
        }
    }

    private record ParsedMessage(String message, String syncJobId) {}
}
