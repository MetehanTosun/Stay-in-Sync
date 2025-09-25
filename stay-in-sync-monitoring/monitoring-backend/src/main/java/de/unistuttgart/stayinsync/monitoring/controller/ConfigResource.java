package de.unistuttgart.stayinsync.monitoring.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/config")
@ApplicationScoped
public class ConfigResource {

    @ConfigProperty(name = "grafana.base.url")
    String grafanaBaseUrl;

    @GET
    @Path("/grafanaUrl")
    @Produces(MediaType.TEXT_PLAIN)
    public String getGrafanaUrl() {
        return grafanaBaseUrl;
    }
}

