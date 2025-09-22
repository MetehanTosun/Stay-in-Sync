package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class TargetSystemAasService {

    public TargetSystem validateAasTarget(TargetSystem ts) {
        if (ts == null) throw new CoreManagementException(Response.Status.NOT_FOUND, "Target system not found", "Target system is null");
        if (ts.apiType == null || !"AAS".equalsIgnoreCase(ts.apiType)) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid apiType", "apiType must be AAS");
        }
        if (ts.apiUrl == null || ts.apiUrl.isBlank()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Missing baseUrl", "apiUrl is required for AAS");
        }
        if (ts.aasId == null || ts.aasId.isBlank()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Missing aasId", "aasId is required for AAS");
        }
        Log.debugf("Validated AAS TargetSystem id=%d", ts.id);
        return ts;
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



