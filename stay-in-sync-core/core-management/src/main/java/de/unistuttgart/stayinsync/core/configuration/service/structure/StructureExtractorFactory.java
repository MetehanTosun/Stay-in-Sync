package de.unistuttgart.stayinsync.core.configuration.service.structure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.StructureExtractionException;

@ApplicationScoped
public class StructureExtractorFactory {

    @Inject
    Instance<StructureExtractor> extractors;

    /**
     * Liefert den ersten Extractor, der den Endpoint unterstützt.
     */
    public StructureExtractor getExtractor(SourceSystemEndpoint endpoint) {
        return extractors.stream()
                         .filter(ext -> ext.supports(endpoint))
                         .findFirst()
                         .orElseThrow(() -> new StructureExtractionException(
                             "No extractor available for endpoint type "
                             + endpoint.getSourceSystem().getType()));
    }
}
    
