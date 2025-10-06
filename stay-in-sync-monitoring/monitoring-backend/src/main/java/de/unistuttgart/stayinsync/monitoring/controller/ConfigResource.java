package de.unistuttgart.stayinsync.monitoring.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/config")
@ApplicationScoped
public class ConfigResource {

    @ConfigProperty(name = "grafana.base.url")
    String grafanaBaseUrl;

    @GET
    @Path("/grafanaUrl")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
            summary = "Get Grafana base URL",
            description = "Returns the configured Grafana base URL as defined in the application configuration."
    )
    @APIResponse(
            responseCode = "200",
            description = "The configured Grafana base URL"
    )
    public String getGrafanaUrl() {
        return grafanaBaseUrl;
    }
}
