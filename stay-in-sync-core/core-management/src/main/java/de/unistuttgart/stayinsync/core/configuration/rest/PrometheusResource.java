package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * REST resource that exposes API endpoints for both SourceSystems and TargetSystems,
 * used by Prometheus/Blackbox exporter.
 * - Only HTTP GET endpoints are returned.
 */
@Path("/api/monitoring/prometheus")
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class PrometheusResource {

    // public endpoints

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

    // Flat endpoints for Prometheus

    @GET
    @Path("/flat/sources")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, List<String>>> getFlatSourceEndpoints() {
        List<String> urls = SourceSystemEndpoint.<SourceSystemEndpoint>listAll()
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
        List<String> urls = TargetSystemEndpoint.<TargetSystemEndpoint>listAll()
                .stream()
                .filter(e -> e.syncSystem != null && notBlank(e.syncSystem.apiUrl) && "GET".equalsIgnoreCase(e.httpRequestType))
                .map(e -> normalizeUrl(e.syncSystem.apiUrl, e.endpointPath))
                .toList();

        return urls.stream()
                .map(url -> Map.of("targets", List.of(url)))
                .toList();
    }

    // PRIVATE HELPERS

    private Map<String, List<String>> buildSourceSystemUrlsBySystem() {
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.listAll();
        List<SourceSystemEndpoint> getEndpoints = endpoints.stream().filter(this::isGet).toList();

        return getEndpoints.stream()
                .collect(Collectors.groupingBy(
                        e -> safeName(e.sourceSystem),
                        Collectors.mapping(
                                e -> normalizeUrl(e.sourceSystem.apiUrl, e.endpointPath),
                                collectDistinctPreservingOrder()
                        )
                ));
    }

    private Map<String, List<String>> buildTargetSystemUrlsBySystem() {
        List<TargetSystemEndpoint> endpoints = TargetSystemEndpoint.listAll();
        List<TargetSystemEndpoint> getEndpoints = endpoints.stream().filter(this::isGet).toList();

        return getEndpoints.stream()
                .filter(e -> e.targetSystem != null && notBlank(e.targetSystem.apiUrl) && notBlank(e.endpointPath))
                .collect(Collectors.groupingBy(
                        e -> safeName(e.targetSystem),
                        Collectors.mapping(
                                e -> normalizeUrl(e.targetSystem.apiUrl, e.endpointPath),
                                collectDistinctPreservingOrder()
                        )
                ));
    }

    private boolean isGet(Object endpoint) {
        Object method;
        if (endpoint instanceof SourceSystemEndpoint sse) {
            method = sse.httpRequestType;
        } else if (endpoint instanceof TargetSystemEndpoint tse) {
            method = tse.httpRequestType;
        } else {
            return false;
        }
        return method != null && "GET".equalsIgnoreCase(method.toString());
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

        if (!right.isEmpty() && left.endsWith(right)) {
            return left;
        }

        if (left.endsWith("/") && right.startsWith("/")) {
            return left + right.substring(1);
        } else if (!left.endsWith("/") && !right.startsWith("/")) {
            return left + "/" + right;
        }
        return left + right;
    }

    private List<String> concatValues(Map<String, List<String>> a, Map<String, List<String>> b) {
        return new ArrayList<>() {{
            a.values().forEach(this::addAll);
            b.values().forEach(this::addAll);
        }};
    }

    private List<String> flatten(Map<String, List<String>> map) {
        return map.values().stream().flatMap(Collection::stream).distinct().toList();
    }

    private static <T> Collector<T, LinkedHashSet<T>, List<T>> collectDistinctPreservingOrder() {
        return new Collector<>() {

            @Override
            public Supplier<LinkedHashSet<T>> supplier() {
                return LinkedHashSet::new;
            }

            @Override
            public BiConsumer<LinkedHashSet<T>, T> accumulator() {
                return LinkedHashSet::add;
            }

            @Override
            public BinaryOperator<LinkedHashSet<T>> combiner() {
                return (left, right) -> {
                    left.addAll(right); return left;
                };
            }
            @Override
            public Function<LinkedHashSet<T>, List<T>> finisher() {
                return ArrayList::new;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        };
    }

    public record HealthcheckDiscoveryDTO(
            Map<String, List<String>> sourceSystems,
            Map<String, List<String>> targetSystems,
            List<String> all
    ) {}
}


