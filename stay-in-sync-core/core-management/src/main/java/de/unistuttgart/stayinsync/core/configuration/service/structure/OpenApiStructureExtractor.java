package de.unistuttgart.stayinsync.core.configuration.service.structure;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.StructureExtractionException;
import de.unistuttgart.stayinsync.core.configuration.repository.SourceSystemEndpointRepository;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemType;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.PathItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts all endpoints and JSON schemas from an OpenAPI specification.
 */
@ApplicationScoped
public class OpenApiStructureExtractor implements StructureExtractor {

    @Inject
    SourceSystemEndpointRepository endpointRepo;

    @Override
    public boolean supports(SourceSystemEndpoint endpoint) {
        // only support endpoints whose parent SourceSystem has REST_OPENAPI type
        return endpoint.getSourceSystem() != null
            && endpoint.getSourceSystem().getType() == SourceSystemType.REST_OPENAPI;
    }

    /**
     * Parses the OpenAPI spec from the SourceSystem and creates endpoint entities.
     */
    public List<SourceSystemEndpoint> extractEndpoints(SourceSystem system) {
        try {
            String spec = system.getOpenApi();
            OpenAPI api = new OpenAPIV3Parser().readContents(spec, null, null).getOpenAPI();
            if (api == null) {
                throw new StructureExtractionException("Failed to parse OpenAPI spec");
            }
            List<SourceSystemEndpoint> endpoints = new ArrayList<>();
            Paths paths = api.getPaths();
            if (paths != null) {
                for (String path : paths.keySet()) {
                    PathItem item = paths.get(path);
                    item.readOperationsMap().forEach((method, op) -> {
                        SourceSystemEndpoint e = new SourceSystemEndpoint();
                        e.setSourceSystem(system);
                        e.setEndpointPath(path);
                        e.setHttpRequestType(method.name());
                        e.setPollingActive(false);
                        e.setSchemaMode("manual");
                        endpoints.add(e);
                    });
                }
            }
            return endpoints;
        } catch (Exception e) {
            throw new StructureExtractionException(
                "Error extracting endpoints from OpenAPI spec", e);
        }
    }

    @Override
    public String extractSchema(SourceSystemEndpoint endpoint) throws StructureExtractionException {
        try {
            String spec = endpoint.getSourceSystem().getOpenApi();
            OpenAPI api = new OpenAPIV3Parser().readContents(spec, null, null).getOpenAPI();
            if (api == null) {
                throw new StructureExtractionException("Failed to parse OpenAPI spec");
            }
            Paths paths = api.getPaths();
            PathItem item = paths.get(endpoint.getEndpointPath());
            if (item == null) {
                throw new StructureExtractionException(
                    "Path not found in spec: " + endpoint.getEndpointPath());
            }
            switch (endpoint.getHttpRequestType()) {
                case "GET":
                    return item.getGet().getResponses().toString();
                case "POST":
                    return item.getPost().getResponses().toString();
                case "PUT":
                    return item.getPut().getResponses().toString();
                case "DELETE":
                    return item.getDelete().getResponses().toString();
                default:
                    throw new StructureExtractionException(
                        "Unsupported method: " + endpoint.getHttpRequestType());
            }
        } catch (Exception e) {
            throw new StructureExtractionException(
                "Error extracting JSON schema", e);
        }
    }
}
