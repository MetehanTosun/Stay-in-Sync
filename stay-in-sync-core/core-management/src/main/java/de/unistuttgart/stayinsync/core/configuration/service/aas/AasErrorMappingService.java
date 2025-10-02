package de.unistuttgart.stayinsync.core.configuration.service.aas;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/**
 * Common AAS error mapping service for both Source and Target systems.
 * Provides shared HTTP error mapping logic to eliminate code duplication.
 */
@ApplicationScoped
public class AasErrorMappingService {

    /**
     * Maps HTTP status codes to appropriate JAX-RS responses.
     * 
     * @param statusCode the HTTP status code
     * @param statusMessage the HTTP status message
     * @param body the response body
     * @return the mapped JAX-RS response
     */
    public Response mapHttpError(int statusCode, String statusMessage, String body) {
        String message = body != null && !body.isBlank() ? body : (statusMessage != null ? statusMessage : "Upstream error");
        return switch (statusCode) {
            case 401 -> Response.status(Response.Status.UNAUTHORIZED).entity(message).build();
            case 403 -> Response.status(Response.Status.FORBIDDEN).entity(message).build();
            case 404 -> Response.status(Response.Status.NOT_FOUND).entity(message).build();
            case 405 -> Response.status(Response.Status.METHOD_NOT_ALLOWED).entity(message).build();
            case 409 -> Response.status(Response.Status.CONFLICT).entity(message).build();
            case 504 -> Response.status(Response.Status.GATEWAY_TIMEOUT).entity(message).build();
            default -> Response.status(statusCode).entity(message).build();
        };
    }
}
