package de.unistuttgart.stayinsync.core.configuration.service;

import jakarta.enterprise.context.ApplicationScoped;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class SourceSystemEndpointService {

    /**
     * Retrieves all endpoints for a given source system.
     *
     * @param sourceId the ID of the source system
     * @return list of endpoints belonging to the source system
     */
    public List<SourceSystemEndpoint> listBySourceId(Long sourceId) {
        return SourceSystemEndpoint.list("sourceSystem.id", sourceId);
    }

    /**
     * Extracts and persists the JSON schema for a specific endpoint.
     *
     * @param endpointId the ID of the endpoint
     * @return the updated endpoint entity with jsonSchema and schemaMode set
     */
    @Transactional
    public SourceSystemEndpoint extractSchema(Long endpointId) {
        SourceSystemEndpoint endpoint = SourceSystemEndpoint.findById(endpointId);
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint not found: " + endpointId);
        }
        // TODO: Replace stub with actual StructureExtractor logic
        String schema = "{}";
        endpoint.jsonSchema = schema;
        endpoint.schemaMode = "auto";
        endpoint.persist();
        return endpoint;
    }
}
