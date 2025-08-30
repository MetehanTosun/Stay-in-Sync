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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class LogService {

    private static final String LOKI_URL = "http://localhost:3100/loki/api/v1/query_range";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LogService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public List<LogEntryDto> fetchAndParseLogs(String syncJobId, long startNs, long endNs, String level) {
        try {
            // Labels zusammenbauen
            List<String> labels = new ArrayList<>();
            if (syncJobId != null && !syncJobId.isBlank()) {
                labels.add("syncJobId=\"" + syncJobId + "\"");
            } else {
                labels.add("agent=\"fluent-bit\"");
            }
            if (level != null && !level.isBlank()) {
                labels.add("level=\"" + level.toUpperCase() + "\"");
            }

            // Query: labelSelector
            String query = "{" + String.join(",", labels) + "}";

            // URL bauen
            String url = String.format("%s?query=%s&start=%d&end=%d&limit=1000&direction=backward",
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

            if (response.statusCode() != 200) {
                throw new RuntimeException("Loki call failed: " + response.statusCode() + " - " + response.body());
            }

            // JSON parsen
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
                        // Outer JSON im value[1] parsen
                        JsonNode messageNode = objectMapper.readTree(messageJson);

                        // Normale Message übernehmen
                        message = messageNode.path("message").asText(messageJson);

                        // syncJobId direkt auslesen (falls vorhanden)
                        parsedSyncJobId = messageNode.path("syncJobId").asText(null);

                        // Falls "message" selbst noch JSON enthält → optional nochmal reinschauen
                        if (message.startsWith("{") && message.endsWith("}")) {
                            try {
                                JsonNode innerNode = objectMapper.readTree(message);
                                if (innerNode.has("syncJobId")) {
                                    parsedSyncJobId = innerNode.get("syncJobId").asText();
                                }
                                message = innerNode.path("message").asText(message);
                            } catch (Exception ignore) {
                                // kein valides Inner-JSON → ignorieren
                            }
                        }
                    } catch (Exception ex) {
                        // Falls gar kein JSON → den Rohstring als Message nehmen
                        message = messageJson;
                    }

                    logs.add(new LogEntryDto(service, lvl, message, timestamp, parsedSyncJobId));
                }
            }

            return logs;

        } catch (Exception e) {
            Log.error("Fehler", e);
            throw new RuntimeException("Error fetching or parsing logs", e);
        }
    }

    /**
     * Neuer Service-Methode: Logs für mehrere TransformationIds abrufen
     */
    public List<LogEntryDto> fetchAndParseLogsForTransformations(List<String> transformationIds, long startNs, long endNs, String level) {
        try {
            if (transformationIds == null || transformationIds.isEmpty()) {
                return new ArrayList<>();
            }

            // Labels für Loki-Abfrage
            List<String> labels = new ArrayList<>();
            labels.add("agent=\"fluent-bit\"");

            // Level-Label nur hinzufügen, wenn ein spezifisches Level gewählt wurde

            if (level != null && !level.isBlank()) {
                labels.add("level=\"" + level.toUpperCase() + "\"");
            }

            // TransformationIds als Regex im Loki-Labelfilter
            String regex = String.join("|", transformationIds);
            labels.add("transformationId=~\"" + regex + "\"");

            // Query zusammenbauen
            String query = "{" + String.join(",", labels) + "}";

            String url = String.format("%s?query=%s&start=%d&end=%d&limit=1000&direction=backward",
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

            // JSON parsen (analog zu fetchAndParseLogs)
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

            logs.sort(Comparator.comparingLong(a -> Long.parseLong(a.timestamp())));

            return logs;
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen von Logs für TransformationIds", e);
            throw new RuntimeException(e);
        }
    }





    //TODO:Fix
    public List<String> fetchErrorSyncJobIds(long startNs, long endNs) {
        List<LogEntryDto> errorLogs = null;

        return errorLogs.stream()
                .map(LogEntryDto::transformationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

}
