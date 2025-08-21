package de.unistuttgart.stayinsync.monitoring.controller;

import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.io.PrintWriter;
import java.net.Socket;

@Path("/test-log")
public class TestLogResource {

    @GET
    public String sendTestLog(@QueryParam("syncJobId") String syncJobId) {
        if (syncJobId == null || syncJobId.isBlank()) {
            syncJobId = "test-job-123"; // Default-Wert zum Testen
        }

        // JSON-Log direkt mit allen gewünschten Feldern
        String jsonLog = String.format(
                "{" +

                        "Test log message for Loki\"," +
                        "\"syncJobId\":\"%s\"" +
                        "}",
                syncJobId
        );

        // Direkt als Log ausgeben
        Log.error(jsonLog);


        return "Sent test log with syncJobId=" + syncJobId;
    }
}
