package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig.ApiKeyAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig.BasicAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig.SyncSystemAuthConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility service responsible for building HTTP headers used in AAS (Asset Administration Shell) API requests.
 * Merges custom API headers, authentication headers (Basic, API Key), and standard JSON headers based on mode.
 */
@ApplicationScoped
public class HttpHeaderBuilder {

    /**
     * Defines operational modes for building HTTP headers.
     * READ: Only includes Accept header.
     * WRITE_JSON: Includes both Accept and Content-Type headers for JSON requests.
     */
    public enum Mode { READ, WRITE_JSON }

    /**
     * Builds and merges all HTTP headers required for communication with a SyncSystem (AAS source or target system).
     * Combines system-level authentication headers, custom API headers from persistence, and standard JSON headers.
     *
     * @param system The SyncSystem for which headers are generated. May include authentication configuration.
     * @param mode The header generation mode (READ or WRITE_JSON).
     * @return A map of HTTP header key-value pairs ready for use in API requests.
     */
    public Map<String, String> buildMergedHeaders(SyncSystem system, Mode mode) {
        Map<String, String> headers = new HashMap<>();
        if (mode == Mode.READ) {
            headers.putIfAbsent("Accept", "application/json");
        } else if (mode == Mode.WRITE_JSON) {
            headers.putIfAbsent("Accept", "application/json");
            headers.putIfAbsent("Content-Type", "application/json");
        }

        if (system == null) return headers;

        try {
            List<ApiHeader> customHeaders = ApiHeader.findBySyncSystemId(system.id);
            if (customHeaders != null) {
                for (ApiHeader h : customHeaders) {
                    if (h.headerName != null && !h.values.isEmpty()) {
                        headers.putIfAbsent(h.headerName, String.join(",", h.values));
                    }
                }
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to load ApiHeaders for system %d", system.id);
        }

        SyncSystemAuthConfig auth = system.authConfig;
        if (auth instanceof BasicAuthConfig basic) {
            String user = basic.username != null ? basic.username : "";
            String pass = basic.password != null ? basic.password : "";
            String token = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
            headers.putIfAbsent("Authorization", "Basic " + token);
        } else if (auth instanceof ApiKeyAuthConfig apiKey) {
            String name = apiKey.headerName != null ? apiKey.headerName : "X-API-KEY";
            headers.putIfAbsent(name, apiKey.apiKey != null ? apiKey.apiKey : "");
        }

        return headers;
    }
}


