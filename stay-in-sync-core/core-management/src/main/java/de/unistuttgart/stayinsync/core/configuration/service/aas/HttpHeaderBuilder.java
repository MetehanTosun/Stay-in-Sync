package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.ApiKeyAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.BasicAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.SyncSystemAuthConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HttpHeaderBuilder {

    public enum Mode { READ, WRITE_JSON }

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


