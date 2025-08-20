package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

@ApplicationScoped
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    public List<LogEntryDto> fetchAndParseLogs(String syncJobId, Long startTimeNs, Long endTimeNs, String level) {
        // 1. Logs aus Loki holen (HTTP Call)
        JSONArray rawLogs = fetchFromLoki(syncJobId, startTimeNs, endTimeNs, level);

        // 2. Parsen und Filtern
        if (rawLogs.isEmpty()) {
            log.info("No logs found for the given query.");
            return Collections.emptyList();
        }

        List<LogEntryDto> parsedLogs = new ArrayList<>();
        for (int i = 0; i < rawLogs.length(); i++) {
            JSONObject entry = rawLogs.getJSONObject(i);
            String rawMessage = entry.getString("message");

            // Level extrahieren
            String entryLevel = Optional.ofNullable(extractValue(rawMessage, "level"))
                    .map(String::toLowerCase)
                    .orElse("unknown");


            // Message extrahieren
            String message = extractQuotedValue(rawMessage, "msg");
            if (message == null) message = extractQuotedValue(rawMessage, "message");

            // Component & Caller
            String component = extractValue(rawMessage, "component");
            String caller = extractValue(rawMessage, "caller");

            LogEntryDto logEntry = new LogEntryDto(
                    entry.getString("timestamp"),
                    message,
                    rawMessage,
                    entryLevel,
                    caller
            );
            parsedLogs.add(logEntry);
        }

        // 3. Sortieren nach Zeitstempel absteigend
        parsedLogs.sort(Comparator.comparingLong(LogEntryDto::getTimestamp).reversed());

        return parsedLogs;
    }

    private JSONArray fetchFromLoki(String nodeId, Long startTimeNs, Long endTimeNs, String level) {
        // Labels zusammenbauen
        StringBuilder labelParts = new StringBuilder("agent=\"" + "fluent-bit" + "\"");
        if (nodeId != null && !nodeId.isEmpty()) {
            labelParts.append(",nodeId=\"").append(nodeId).append("\"");
        }
        String labelSelector = "{" + labelParts + "}";

        // Query zusammensetzen (inkl. optionalem Level)
        String query = (level != null && !level.isEmpty())
                ? labelSelector + " |= \"level=" + level + "\""
                : labelSelector;

        try {
            // Query encoden, damit Loki sie versteht
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);

            String params = String.format(
                    "?query=%s&start=%d&end=%d&limit=1000&direction=backward",
                    encodedQuery, startTimeNs, endTimeNs
            );

            String baseUrl = "http://localhost:3100/loki/api/v1/query_range";
            java.net.URL url = new URI(baseUrl + params).toURL();
            Log.info("Fetching logs from Loki: " + url);

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            try (java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray result = new JSONArray();

                if (json.has("data") && json.getJSONObject("data").has("result")) {
                    JSONArray streams = json.getJSONObject("data").getJSONArray("result");
                    for (int i = 0; i < streams.length(); i++) {
                        JSONObject streamJson = streams.getJSONObject(i);
                        JSONArray values = streamJson.getJSONArray("values");
                        for (int j = 0; j < values.length(); j++) {
                            JSONArray entry = values.getJSONArray(j);
                            JSONObject entryObj = new JSONObject();
                            entryObj.put("timestamp", entry.getString(0)); // ns timestamp
                            entryObj.put("message", entry.getString(1));   // log line
                            result.put(entryObj);
                        }
                    }
                }

                return result;
            }
        } catch (Exception e) {
            log.error("Error fetching logs from Loki", e);
            return new JSONArray();
        }
    }

    private String extractValue(String text, String key) {
        var matcher = java.util.regex.Pattern.compile(key + "=(\\S+)").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractQuotedValue(String text, String key) {
        var matcher = java.util.regex.Pattern.compile(key + "=\"([^\"]+)\"").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
