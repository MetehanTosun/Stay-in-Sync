package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class SourceSystemAasService {

    public SourceSystem validateAasSource(SourceSystem ss) {
        if (ss == null) throw new CoreManagementException(Response.Status.NOT_FOUND, "Source system not found", "Source system is null");
        if (ss.apiType == null || !"AAS".equalsIgnoreCase(ss.apiType)) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid apiType", "apiType must be AAS");
        }
        if (ss.apiUrl == null || ss.apiUrl.isBlank()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Missing baseUrl", "apiUrl is required for AAS");
        }
        if (ss.aasId == null || ss.aasId.isBlank()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Missing aasId", "aasId is required for AAS");
        }
        Log.debugf("Validated AAS SourceSystem id=%d", ss.id);
        return ss;
    }

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


