package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * Target System AAS Service.
 * Delegates to common AAS services to eliminate code duplication.
 */
@ApplicationScoped
public class TargetSystemAasService {

    @Inject
    AasValidationService validationService;

    @Inject
    AasErrorMappingService errorMappingService;

    /**
     * Validates a TargetSystem for AAS operations.
     * Delegates to the common validation service.
     * 
     * @param targetSystem the target system to validate
     * @return the validated target system
     */
    public TargetSystem validateAasTarget(TargetSystem targetSystem) {
        return validationService.validateAasTarget(targetSystem);
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



