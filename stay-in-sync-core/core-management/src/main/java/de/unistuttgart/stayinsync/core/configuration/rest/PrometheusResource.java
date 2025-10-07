package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Path("/api/monitoring/prometheus")
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class PrometheusResource {

    @GET
    @Path("/flat/sources")
    @Produces(MediaType.APPLICATION_JSON)
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

    @GET
    @Path("/flat/targets")
    @Produces(MediaType.APPLICATION_JSON)
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


    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String normalizeHealthUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String url = baseUrl.trim();

        // Wenn die URL bereits einen Pfad enthält, nichts anhängen
        if (url.matches(".+/.+")) {
            return url;
        }

        // ansonsten Standard /health anhängen
        if (url.endsWith("/")) {
            return url + "health";
        } else {
            return url + "/health";
        }
    }

}
