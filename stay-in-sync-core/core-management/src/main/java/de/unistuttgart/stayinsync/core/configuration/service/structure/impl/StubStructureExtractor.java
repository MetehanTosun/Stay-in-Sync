package de.unistuttgart.stayinsync.core.configuration.service.structure.impl;

import jakarta.enterprise.context.ApplicationScoped;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.service.structure.StructureExtractionException;
import de.unistuttgart.stayinsync.core.configuration.service.structure.StructureExtractor;

/**
 * Stub-Implementation des Extractors, gibt derzeit nur ein leeres Schema zurück.
 */
@ApplicationScoped
public class StubStructureExtractor implements StructureExtractor {

    @Override
    public boolean supports(SourceSystemEndpoint endpoint) {
        // Unterstützt vorerst alle Endpunkte
        return true;
    }

    @Override
    public String extractSchema(SourceSystemEndpoint endpoint) throws StructureExtractionException {
        // Platzhalter-Logik: später replace durch echte Extraktion
        return "{}";
    }
}