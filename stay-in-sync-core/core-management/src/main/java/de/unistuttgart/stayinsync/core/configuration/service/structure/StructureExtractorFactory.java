package de.unistuttgart.stayinsync.core.configuration.service.structure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;

import de.unistuttgart.stayinsync.core.configuration.service.structure.StructureExtractor;
import de.unistuttgart.stayinsync.core.configuration.service.structure.StructureExtractionException;

@ApplicationScoped
public class StructureExtractorFactory {

    @Inject
    Instance<StructureExtractor> extractors;

    /**
     * Wählt anhand supports() den passenden Extractor aus.
     */
    public StructureExtractor getExtractor(SourceSystemEndpoint endpoint) {
        return extractors.stream()
                         .filter(ext -> ext.supports(endpoint))
                         .findFirst()
                         .orElseThrow(() -> new StructureExtractionException(
                             "Kein geeigneter Extractor für Endpoint " + endpoint.id));
    }
}