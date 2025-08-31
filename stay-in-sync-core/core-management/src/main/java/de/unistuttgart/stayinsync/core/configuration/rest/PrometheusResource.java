package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Path("/api/monitoring/prometheus")
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class PrometheusResource {

    // --- Public Endpoints ---

    @GET
    @Path("/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllEndpoints() {
        Log.debug("Collecting GET endpoints for source and target systems.");

        Map<String, List<String>> sources = buildSourceSystemUrlsBySystem();
        Map<String, List<String>> targets = buildTargetSystemUrlsBySystem();

        List<String> all = new ArrayList<>(new LinkedHashSet<>(concatValues(sources, targets)));

        HealthcheckDiscoveryDTO dto = new HealthcheckDiscoveryDTO(sources, targets, all);
        return Response.ok(dto).build();
    }

    @GET
    @Path("/endpoints/sources")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSourceEndpoints() {
        return Response.ok(buildSourceSystemUrlsBySystem()).build();
    }

    @GET
    @Path("/endpoints/targets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTargetEndpoints() {
        return Response.ok(buildTargetSystemUrlsBySystem()).build();
    }

    @GET
    @Path("/flat/sources")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, List<String>>> getFlatSourceEndpoints() {
        List<String> urls = SourceSystemEndpoint.listAll()
                .stream()
                .filter(e -> e.syncSystem != null && notBlank(e.syncSystem.apiUrl) && "GET".equalsIgnoreCase(e.httpRequestType))
                .map(e -> normalizeUrl(e.syncSystem.apiUrl, e.endpointPath))
                .toList();

        return urls.stream()
                .map(url -> Map.of("targets", List.of(url)))
                .toList();
    }

    @GET
    @Path("/flat/targets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, List<String>>> getFlatTargetEndpoints() {
        List<String> urls = TargetSystemEndpoint.listAll()
                .stream()
                .filter(e -> e.syncSystem != null && notBlank(e.syncSystem.apiUrl) && "GET".equalsIgnoreCase(e.httpRequestType))
                .map(e -> normalizeUrl(e.syncSystem.apiUrl, e.endpointPath))
                .toList();

        return urls.stream()
                .map(url -> Map.of("targets", List.of(url)))
                .toList();
    }

    // --- Private Helpers ---

    private Map<String, List<String>> buildSourceSystemUrlsBySystem() {
        List<SourceSystemEndpoint> getEndpoints = SourceSystemEndpoint.listAll()
                .stream()
                .filter(this::isGet)
                .filter(e -> e.syncSystem != null && notBlank(e.syncSystem.apiUrl) && notBlank(e.endpointPath))
                .toList();

        return getEndpoints.stream()
                .collect(Collectors.groupingBy(
                        e -> safeName(e.syncSystem),
                        Collectors.mapping(
                                e -> normalizeUrl(e.syncSystem.apiUrl, e.endpointPath),
                                collectDistinctPreservingOrder()
                        )
                ));
    }

    private Map<String, List<String>> buildTargetSystemUrlsBySystem() {
        List<TargetSystemEndpoint> getEndpoints = TargetSystemEndpoint.listAll()
                .stream()
                .filter(this::isGet)
                .filter(e -> e.syncSystem != null && notBlank(e.syncSystem.apiUrl) && notBlank(e.endpointPath))
                .toList();

        return getEndpoints.stream()
                .collect(Collectors.groupingBy(
                        e -> safeName(e.syncSystem),
                        Collectors.mapping(
                                e -> normalizeUrl(e.syncSystem.apiUrl, e.endpointPath),
                                collectDistinctPreservingOrder()
                        )
                ));
    }

    private boolean isGet(Object endpoint) {
        String method = null;
        if (endpoint instanceof SourceSystemEndpoint sse) {
            method = sse.httpRequestType;
        } else if (endpoint instanceof TargetSystemEndpoint tse) {
            method = tse.httpRequestType;
        }
        return method != null && "GET".equalsIgnoreCase(method);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String safeName(SourceSystem s) {
        return notBlank(s.name) ? s.name : ("source-" + s.id);
    }

    private String safeName(TargetSystem s) {
        return notBlank(s.name) ? s.name : ("target-" + s.id);
    }

    private String normalizeUrl(String baseUrl, String path) {
        String left = baseUrl == null ? "" : baseUrl.trim();
        String right = path == null ? "" : path.trim();

        if (!right.isEmpty() && left.endsWith(right)) return left;
        if (left.endsWith("/") && right.startsWith("/")) return left + right.substring(1);
        if (!left.endsWith("/") && !right.startsWith("/")) return left + "/" + right;
        return left + right;
    }

    private List<String> concatValues(Map<String, List<String>> a, Map<String, List<String>> b) {
        List<String> combined = new ArrayList<>();
        a.values().forEach(combined::addAll);
        b.values().forEach(combined::addAll);
        return combined;
    }

    private static <T> Collector<T, LinkedHashSet<T>, List<T>> collectDistinctPreservingOrder() {
        return Collector.of(
                LinkedHashSet::new,
                LinkedHashSet::add,
                (left, right) -> { left.addAll(right); return left; },
                ArrayList::new
        );
    }

    public record HealthcheckDiscoveryDTO(
            Map<String, List<String>> sourceSystems,
            Map<String, List<String>> targetSystems,
            List<String> all
    ) {}
}



