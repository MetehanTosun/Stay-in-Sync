package de.unistuttgart.stayinsync.monitoring.controller;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
@Path("/api/replay")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReplayResource {

    @ConfigProperty(name = "backend-api/mp-rest/url")
    String coreManagementUrl;

    private final HttpClient client = HttpClient.newHttpClient();

    @POST
    @Path("/execute")
    @Operation(summary = "Führt einen Replay-Vorgang im Core Management Service aus")
    @APIResponse(
            responseCode = "200",
            description = "Replay erfolgreich ausgeführt",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "success-response",
                            value = "{\"status\":\"success\",\"message\":\"Replay executed successfully\"}"
                    )
            )
    )
    @APIResponse(
            responseCode = "500",
            description = "Fehler beim Proxy-Aufruf",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "error-response",
                            value = "{\"error\":\"Proxy call failed\"}"
                    )
            )
    )
    public Response executeReplay(String dto) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(coreManagementUrl + "/api/replay/execute"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(dto))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return Response
                    .status(response.statusCode())
                    .entity(response.body())
                    .build();

        } catch (Exception e) {
            Log.error(e.getMessage());
            return Response.serverError().entity("{\"error\":\"Proxy call failed\"}").build();
        }
    }

    @GET
    @Path("/{transformationId}")
    @Operation(summary = "Ruft das Replay-Skript für eine gegebene Transformations-ID aus dem Core Management Service ab")
    @APIResponse(
            responseCode = "200",
            description = "Skript erfolgreich abgerufen",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "script-response",
                            value = "{\"id\":\"1234\",\"script\":\"print('Hello World')\"}"
                    )
            )
    )
    @APIResponse(
            responseCode = "500",
            description = "Fehler beim Proxy-Aufruf",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "error-response",
                            value = "{\"error\":\"Proxy call failed\"}"
                    )
            )
    )
    public Response getScriptByTransformationId(@PathParam("transformationId") String transformationId) {
        try {
            String url = coreManagementUrl + "/api/config/transformation/" + transformationId + "/script";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return Response
                    .status(response.statusCode())
                    .entity(response.body())
                    .build();

        } catch (Exception e) {
            Log.error(e.getMessage());
            return Response.serverError().entity("{\"error\":\"Proxy call failed\"}").build();
        }
    }
}
