package de.unistuttgart.stayinsync.monitoring.controller;

import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.HashMap;
import java.util.Map;

@Path("/test-log")
public class TestLogResource {


        @GET
        public String sendTestLog(@QueryParam("syncJobId") String syncJobId) {

            if (syncJobId == null || syncJobId.isBlank()) {
                syncJobId = "test-job-123"; // Default-Wert zum Testen
            }

            // JSON-ähnliche Struktur als Map erzeugen
            Map<String, Object> logPayload = new HashMap<>();
            logPayload.put("level", "INFO");          // von Loki als Label 'level' auswertbar
            logPayload.put("message", "Test log message for Loki");
            logPayload.put("traceID", "trace-abc-001");
            logPayload.put("component", "test-controller");

            // Log als JSON (Quarkus wandelt Map.toString nicht automatisch zu JSON)
            // Daher String.format mit explizitem JSON
            String jsonLog = String.format(
                    "{\"level\":\"%s\",\"message\":\"%s\",\"traceID\":\"%s\",\"component\":\"%s\"}",
                    logPayload.get("level"),
                    logPayload.get("message"),
                    logPayload.get("traceID"),
                    logPayload.get("component")
            );

            // Direkt als einzelnes JSON-Log ausgeben
            Log.info(jsonLog);

            return "Sent test log with syncJobId=" + syncJobId;
        }

}
