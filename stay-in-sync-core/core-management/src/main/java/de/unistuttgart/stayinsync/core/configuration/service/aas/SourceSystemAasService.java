package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
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
     * Throws CoreManagementWebException for HTTP errors.
     * Uses the proper exception handling mechanism instead of manual response mapping.
     * 
     * @param statusCode the HTTP status code
     * @param statusMessage the HTTP status message
     * @param body the response body
     * @throws CoreManagementWebException with appropriate status and message
     */
    public void throwHttpError(int statusCode, String statusMessage, String body) {
        Response.Status status = mapStatusCode(statusCode);
        String message = body != null && !body.isBlank() ? body : (statusMessage != null ? statusMessage : "Upstream error");
        throw new CoreManagementWebException(status, "AAS Operation Failed", message);
    }

    /**
     * Maps HTTP status codes to JAX-RS Response.Status.
     * 
     * @param statusCode the HTTP status code
     * @return the corresponding Response.Status
     */
    private Response.Status mapStatusCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> Response.Status.BAD_REQUEST;
            case 401 -> Response.Status.UNAUTHORIZED;
            case 403 -> Response.Status.FORBIDDEN;
            case 404 -> Response.Status.NOT_FOUND;
            case 409 -> Response.Status.CONFLICT;
            case 422 -> Response.Status.BAD_REQUEST; // UNPROCESSABLE_ENTITY not available in JAX-RS
            case 500 -> Response.Status.INTERNAL_SERVER_ERROR;
            case 502 -> Response.Status.BAD_GATEWAY;
            case 503 -> Response.Status.SERVICE_UNAVAILABLE;
            default -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }
}


