package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * Source System AAS Service.
 * Delegates to common AAS services to eliminate code duplication.
 */
@ApplicationScoped
public class SourceSystemAasService {

    @Inject
    AasValidationService validationService;

    @Inject
    AasErrorMappingService errorMappingService;

    /**
     * Validates a SourceSystem for AAS operations.
     * Delegates to the common validation service.
     * 
     * @param sourceSystem the source system to validate
     * @return the validated source system
     */
    public SourceSystem validateAasSource(SourceSystem sourceSystem) {
        return validationService.validateAasSource(sourceSystem);
    }

    /**
     * Maps HTTP status codes to appropriate JAX-RS responses.
     * Delegates to the common error mapping service.
     * 
     * @param statusCode the HTTP status code
     * @param statusMessage the HTTP status message
     * @param body the response body
     * @return the mapped JAX-RS response
     */
    public Response mapHttpError(int statusCode, String statusMessage, String body) {
        return errorMappingService.mapHttpError(statusCode, statusMessage, body);
    }
}


