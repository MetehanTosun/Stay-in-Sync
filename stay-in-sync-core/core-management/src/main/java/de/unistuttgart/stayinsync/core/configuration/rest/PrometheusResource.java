package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.ws.rs.core.HttpHeaders;
import java.net.URI;
import java.util.*;

/**
 * REST resource for Prometheus monitoring.
 * Provides endpoints to expose information about source and target systems
 * in a format suitable for Prometheus.
 */
@Path("/api/monitoring/prometheus")
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class PrometheusResource {

    /**
     * Returns a list of source systems in Prometheus format.
     *
     * @return a list of maps containing the URLs of the source systems.
     */
    @GET
    @Path("/flat/sources")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns a list of source systems in Prometheus format")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created sync-job",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid sync-job passed in (or no request body found)"
    )
    public List<Map<String, List<String>>> getFlatSourceSystems() {
        List<String> urls = SourceSystem.<SourceSystem>listAll()
                .stream()
                .filter(s -> notBlank(s.apiUrl))
                .map(s -> normalizeHealthUrl(s.apiUrl))
                .filter(Objects::nonNull)
                .toList();

        return urls.stream()
                .map(url -> Map.of("targets", List.of(url)))
                .toList();
    }

    /**
     * Returns a list of target systems in Prometheus format.
     *
     * @return a list of maps containing the URLs of the target systems.
     */
    @GET
    @Path("/flat/targets")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns a list of target systems in Prometheus format")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created sync-job",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid sync-job passed in (or no request body found)"
    )
    public List<Map<String, List<String>>> getFlatTargetSystems() {
        List<String> urls = TargetSystem.<TargetSystem>listAll()
                .stream()
                .filter(s -> notBlank(s.apiUrl))
                .map(s -> normalizeHealthUrl(s.apiUrl))
                .filter(Objects::nonNull)
                .toList();

        return urls.stream()
                .map(url -> Map.of("targets", List.of(url)))
                .toList();
    }

    /**
     * Checks if a string is not null and not blank.
     *
     * @param s the string to check
     * @return true if the string is not null and not blank, false otherwise
     */
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Normalizes a base URL by appending "/health" if necessary.
     *
     * @param baseUrl the base URL to normalize
     * @return the normalized URL or null if the input is invalid
     */
    private String normalizeHealthUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String url = baseUrl.trim();

        // If the URL already contains a path, do not append anything
        if (url.matches(".+/.+")) {
            return url;
        }

        // Otherwise, append the default /health path
        if (url.endsWith("/")) {
            return url + "health";
        } else {
            return url + "/health";
        }
    }

}
