package de.unistuttgart.stayinsync.monitoring.controller;

import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.io.PrintWriter;
import java.net.Socket;

package de.unistuttgart.stayinsync.monitoring.controller;

import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

/**
 * REST resource for sending test logs to the logging system (e.g., Loki).
 * Provides an endpoint to generate and emit test logs for monitoring and verification purposes.
 */
@Path("/api/test-log")
public class TestLogResource {

    /**
     * Sends a test log message containing the provided syncJobId.
     * @param syncJobId optional identifier for the sync job; defaults to "test-job-123" if not provided
     * @return a message indicating that the test log has been sent
     */
    @GET
    public String sendTestLog(@QueryParam("syncJobId") String syncJobId) {
        // Provide a default syncJobId if none is supplied
        if (syncJobId == null || syncJobId.isBlank()) {
            syncJobId = "test-job-123";
        }

        // Construct JSON-formatted log message
        String jsonLog = String.format(
                "{\"message\":\"Test log message for Loki\",\"syncJobId\":\"%s\"}",
                syncJobId
        );

        // Emit the log to the logging system
        Log.error(jsonLog);

        return "Sent test log with syncJobId=" + syncJobId;
    }
}