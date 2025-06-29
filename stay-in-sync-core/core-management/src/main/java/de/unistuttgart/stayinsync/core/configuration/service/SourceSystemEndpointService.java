package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.repository.SourceSystemEndpointRepository;
import de.unistuttgart.stayinsync.core.configuration.repository.SourceSystemRepository;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.DiscoveredEndpoint;
import de.unistuttgart.stayinsync.core.configuration.service.structure.StructureExtractorFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing source system endpoints, including manual creation,
 * schema extraction, and discovery from OpenAPI specifications.
 */
@ApplicationScoped
public class SourceSystemEndpointService {

    @Inject
    SourceSystemEndpointRepository repo;

    @Inject
    SourceSystemRepository ssRepo; // neues Repository, um das übergeordnete System zu laden

    @Inject
    StructureExtractorFactory extractorFactory;

    /**
     * List all endpoints associated with a given source system.
     *
     * @param sourceSystemId the ID of the source system
     * @return list of SourceSystemEndpoint entities
     */
    public List<SourceSystemEndpoint> listBySourceId(Long sourceSystemId) {
        return repo.listBySourceSystemId(sourceSystemId);
    }

    /**
     * Extract JSON schema for an existing endpoint using the configured extractor.
     *
     * @param endpointId the ID of the endpoint
     * @return the updated SourceSystemEndpoint with schema populated
     * @throws CoreManagementWebException if the endpoint does not exist
     */
    @Transactional
    public SourceSystemEndpoint extractSchema(Long endpointId) {
        SourceSystemEndpoint endpoint = repo.findByEndpointId(endpointId)
            .orElseThrow(() -> new CoreManagementWebException(
                404,
                "Endpoint not found",
                "No endpoint with id %d", endpointId));

        var extractor = extractorFactory.getExtractor(endpoint);
        String schema = extractor.extractSchema(endpoint);
        endpoint.setJsonSchema(schema);
        endpoint.setSchemaMode("auto");
        // Panache persist automatisch
        return endpoint;
    }

    /**
     * Create a new endpoint for a source system.
     *
     * @param sourceSystemId ID of the parent source system
     * @param endpointPath   the API path (e.g., "/pets")
     * @param httpMethod     HTTP method for this endpoint ("GET", "POST", etc.)
     * @return the newly created SourceSystemEndpoint entity
     * @throws CoreManagementWebException if the parent source system is not found
     */
    @Transactional
    public SourceSystemEndpoint createEndpoint(Long sourceSystemId, String endpointPath, String httpMethod) {
        // 1) Parent-System laden oder 404
        SourceSystem parent = ssRepo.findByIdOptional(sourceSystemId)
            .orElseThrow(() -> new CoreManagementWebException(
                404,
                "Source system not found",
                "No source system with id %d", sourceSystemId));

        // 2) neues Entity befüllen
        SourceSystemEndpoint ep = new SourceSystemEndpoint();
        ep.setEndpointPath(endpointPath);
        ep.setHttpRequestType(httpMethod);
        ep.setPollingActive(false);
        ep.setPollingRateInMs(0);
        ep.setSchemaMode("manual");       // default: manuell (kein Schema)
        ep.setJsonSchema(null);
        ep.setSourceSystem(parent);

        // 3) persistieren und zurückgeben
        repo.persist(ep);
        return ep;
    }
       /**
       * Discover all available endpoints from the stored OpenAPI specification.
       *
       * @param sourceSystemId the ID of the source system
       * @return list of DiscoveredEndpoint DTOs representing each path and method
       * @throws CoreManagementWebException if the source system or spec is invalid
       */
   public List<DiscoveredEndpoint> discoverAllEndpoints(Long sourceSystemId) {
        // 1) load the parent system or fail with 404
        SourceSystem system = ssRepo.findByIdOptional(sourceSystemId)
            .orElseThrow(() -> new CoreManagementWebException(
                404,
                "Source system not found",
                "No source system with id %d", sourceSystemId));

        // 2) parse the OpenAPI spec (either from URL or raw text)
        SwaggerParseResult parseResult;
        if (system.getOpenApiSpecUrl() != null && !system.getOpenApiSpecUrl().isBlank()) {
            parseResult = new OpenAPIV3Parser()
                .readLocation(system.getOpenApiSpecUrl(), null, null);
        } else {
            String spec = system.getOpenApi();
            parseResult = new OpenAPIV3Parser()
                .readContents(spec, null, null);
        }
        OpenAPI api = parseResult.getOpenAPI();
        if (api == null || api.getPaths() == null) {
            throw new CoreManagementWebException(
                400,
                "Invalid OpenAPI spec",
                "Failed to parse OpenAPI spec for source system %d: %s", sourceSystemId
            );
        }

        // 3) walk each path/method and collect
        List<DiscoveredEndpoint> out = new ArrayList<>();
        for (Map.Entry<String, PathItem> e : api.getPaths().entrySet()) {
            String path = e.getKey();
            PathItem pi = e.getValue();
            if (pi.getGet()    != null) out.add(new DiscoveredEndpoint(path, "GET"));
            if (pi.getPost()   != null) out.add(new DiscoveredEndpoint(path, "POST"));
            if (pi.getPut()    != null) out.add(new DiscoveredEndpoint(path, "PUT"));
            if (pi.getDelete() != null) out.add(new DiscoveredEndpoint(path, "DELETE"));
            if (pi.getPatch()  != null) out.add(new DiscoveredEndpoint(path, "PATCH"));
            // … etc. for HEAD, OPTIONS if you like
        }

        return out;
    }
}