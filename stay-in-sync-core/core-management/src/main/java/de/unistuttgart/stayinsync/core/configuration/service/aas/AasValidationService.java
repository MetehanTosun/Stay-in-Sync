package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/**
 * Common AAS validation service for both Source and Target systems.
 * Provides shared validation logic to eliminate code duplication.
 */
@ApplicationScoped
public class AasValidationService {

    /**
     * Validates a SourceSystem for AAS operations.
     * 
     * @param sourceSystem the source system to validate
     * @return the validated source system
     * @throws CoreManagementWebException if validation fails
     */
    public SourceSystem validateAasSource(SourceSystem sourceSystem) {
        if (sourceSystem == null) {
            throw new CoreManagementWebException(Response.Status.NOT_FOUND, "Source system not found", "Source system is null");
        }
        if (sourceSystem.apiType == null || !"AAS".equalsIgnoreCase(sourceSystem.apiType)) {
            throw new CoreManagementWebException(Response.Status.BAD_REQUEST, "Invalid apiType", "apiType must be AAS");
        }
        if (sourceSystem.apiUrl == null || sourceSystem.apiUrl.isBlank()) {
            throw new CoreManagementWebException(Response.Status.BAD_REQUEST, "Missing baseUrl", "apiUrl is required for AAS");
        }
        if (sourceSystem.aasId == null || sourceSystem.aasId.isBlank()) {
            throw new CoreManagementWebException(Response.Status.BAD_REQUEST, "Missing aasId", "aasId is required for AAS");
        }
        Log.debugf("Validated AAS SourceSystem id=%d", sourceSystem.id);
        return sourceSystem;
    }

    /**
     * Validates a TargetSystem for AAS operations.
     * 
     * @param targetSystem the target system to validate
     * @return the validated target system
     * @throws CoreManagementWebException if validation fails
     */
    public TargetSystem validateAasTarget(TargetSystem targetSystem) {
        if (targetSystem == null) {
            throw new CoreManagementWebException(Response.Status.NOT_FOUND, "Target system not found", "Target system is null");
        }
        if (targetSystem.apiType == null || !"AAS".equalsIgnoreCase(targetSystem.apiType)) {
            throw new CoreManagementWebException(Response.Status.BAD_REQUEST, "Invalid apiType", "apiType must be AAS");
        }
        if (targetSystem.apiUrl == null || targetSystem.apiUrl.isBlank()) {
            throw new CoreManagementWebException(Response.Status.BAD_REQUEST, "Missing baseUrl", "apiUrl is required for AAS");
        }
        if (targetSystem.aasId == null || targetSystem.aasId.isBlank()) {
            throw new CoreManagementWebException(Response.Status.BAD_REQUEST, "Missing aasId", "aasId is required for AAS");
        }
        Log.debugf("Validated AAS TargetSystem id=%d", targetSystem.id);
        return targetSystem;
    }
}
