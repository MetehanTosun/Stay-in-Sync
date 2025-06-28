package de.unistuttgart.stayinsync.core.configuration.service.structure;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.StructureExtractionException;

/**

Interface for extracting JSON Schemas from source system endpoints.
*/
public interface StructureExtractor {

/**

Indicates whether this extractor supports the given endpoint type.

@param endpoint the source system endpoint to check

@return true if supported, false otherwise
*/
boolean supports(SourceSystemEndpoint endpoint);

/**

Extracts a JSON Schema for the given endpoint configuration.

@param endpoint the source system endpoint

@return a JSON Schema string

@throws StructureExtractionException on failure
*/
String extractSchema(SourceSystemEndpoint endpoint) throws StructureExtractionException;
}

