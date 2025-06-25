package de.unistuttgart.stayinsync.core.configuration.service.structure;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.service.structure.StructureExtractionException;



/**
 * Interface für die Extraktion eines JSON-Schemas aus einem SourceSystemEndpoint.
 */
public interface StructureExtractor {

    /**
     * Erstellt ein JSON-Schema für den übergebenen Endpoint.
     *
     * @param endpoint das zu verarbeitende Endpoint-Objekt
     * @return JSON-Schema als String
     * @throws StructureExtractionException bei Extraktionsfehlern
     */
    String extractSchema(SourceSystemEndpoint endpoint) throws StructureExtractionException;

    /**
     * Liefert true, wenn dieser Extractor für den gegebenen Endpoint-Typ geeignet ist.
     *
     * @param endpoint das Endpoint-Objekt
     * @return true, wenn unterstützt
     */
    boolean supports(SourceSystemEndpoint endpoint);
}