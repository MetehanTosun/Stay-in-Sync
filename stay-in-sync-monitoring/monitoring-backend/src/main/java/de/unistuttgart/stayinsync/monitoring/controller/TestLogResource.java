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
                        "\"level\":\"INFO\"," +
                        "\"message\":\"Test log message for Loki\"," +
                        "\"stream\":\"monitoring-backend\"," +
                        "\"traceID\":\"trace-abc-001\"," +
                        "\"component\":\"test-controller\"," +
                        "\"syncJobId\":\"%s\"" +
                        "}",
                syncJobId
        );

        // Direkt als Log ausgeben
        Log.info(jsonLog);

//        try(Socket socket = new Socket("localhost", 5170);
//            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
//            out.println(jsonLog);
//        } catch(Exception e) {
//            e.printStackTrace();
//        }


        return "Sent test log with syncJobId=" + syncJobId;
    }
}
