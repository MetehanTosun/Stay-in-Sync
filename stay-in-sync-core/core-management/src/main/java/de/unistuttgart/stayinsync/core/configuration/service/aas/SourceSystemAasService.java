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
}


