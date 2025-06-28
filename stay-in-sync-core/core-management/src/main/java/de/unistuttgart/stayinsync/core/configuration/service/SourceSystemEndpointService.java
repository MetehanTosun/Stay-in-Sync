package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.repository.SourceSystemEndpointRepository;
import de.unistuttgart.stayinsync.core.configuration.service.structure.StructureExtractorFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SourceSystemEndpointService {

    @Inject
    SourceSystemEndpointRepository repo;

    @Inject
    StructureExtractorFactory extractorFactory;

    /**
     * Liefert alle Endpunkte eines gegebenen Quellsystems.
     */
    public List<SourceSystemEndpoint> listBySourceId(Long sourceSystemId) {
        return repo.listBySourceSystemId(sourceSystemId);
    }

    /**
     * Extrahiert das JSON-Schema für den Endpoint mit der gegebenen ID.
     * @param endpointId ID des Endpoints
     * @return das aktualisierte Endpoint-Objekt mit gefülltem jsonSchema und schemaMode="auto"
     */
    @Transactional
    public SourceSystemEndpoint extractSchema(Long endpointId) {
        // 1) Endpoint laden oder 404
        SourceSystemEndpoint endpoint = repo.findByEndpointId(endpointId)
                .orElseThrow(() -> new CoreManagementWebException(
                        404,
                        "Endpoint nicht gefunden",
                        "Kein Endpoint mit id %d gefunden", endpointId));

        // 2) passenden Extractor wählen
        var extractor = extractorFactory.getExtractor(endpoint);

        // 3) Schema erzeugen
        String schema = extractor.extractSchema(endpoint);

        // 4) im Entity speichern
        endpoint.jsonSchema = schema;
        endpoint.schemaMode = "auto";

        // 5) persistieren (Panache macht das automatisch am Ende der TX)
        repo.persist(endpoint);

        return endpoint;
    }
}