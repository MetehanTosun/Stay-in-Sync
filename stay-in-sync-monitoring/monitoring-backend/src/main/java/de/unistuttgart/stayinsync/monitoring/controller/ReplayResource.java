package de.unistuttgart.stayinsync.monitoring.controller;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

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

