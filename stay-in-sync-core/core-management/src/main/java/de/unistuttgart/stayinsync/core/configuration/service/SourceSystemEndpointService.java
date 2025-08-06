package de.unistuttgart.stayinsync.core.configuration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.util.TypeScriptTypeGenerator;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class SourceSystemEndpointService {

    @Inject
    Validator validator;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    SourceSystemEndpointFullUpdateMapper sourceSystemEndpointFullMapper;

    @Inject
    TypeScriptTypeGenerator typeScriptTypeGenerator;

    public SourceSystemEndpoint persistSourceSystemEndpoint(@NotNull @Valid CreateSourceSystemEndpointDTO sourceSystemEndpointDTO, Long sourceSystemId) {
        System.out.println("### TEST persist: " + sourceSystemEndpointDTO.requestBodySchema());
        Log.debug("Persisting source-system-endpoint: " + sourceSystemEndpointDTO + ", for source-system with id: " + sourceSystemId);

        if (sourceSystemEndpointDTO.requestBodySchema() != null && !sourceSystemEndpointDTO.requestBodySchema().isBlank()) {
            try {
                objectMapper.readTree(sourceSystemEndpointDTO.requestBodySchema());
            } catch (JsonProcessingException e) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid JSON in requestBodySchema", "Das Feld requestBodySchema enthält kein valides JSON: %s", e.getMessage());
            }
        }

        if (sourceSystemEndpointDTO.responseBodySchema() != null && !sourceSystemEndpointDTO.responseBodySchema().isBlank()) {
            try {
                objectMapper.readTree(sourceSystemEndpointDTO.responseBodySchema());
            } catch (JsonProcessingException e) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid JSON in responseBodySchema", "Das Feld responseBodySchema enthält kein valides JSON: %s", e.getMessage());
            }
        }

        // Upsert-Logik: Prüfe, ob Endpoint existiert
        SourceSystemEndpoint existing = SourceSystemEndpoint.find("sourceSystem.id = ?1 and endpointPath = ?2 and httpRequestType = ?3",
                sourceSystemId,
                sourceSystemEndpointDTO.endpointPath(),
                sourceSystemEndpointDTO.httpRequestType()
        ).firstResult();

        SourceSystem sourceSystem = sourceSystemService.findSourceSystemById(sourceSystemId).orElseThrow(() ->
                new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find Source System", "There is no source-system with id " + sourceSystemId));

        if (existing != null) {
            // Update
            SourceSystemEndpoint updated = sourceSystemEndpointFullMapper.mapToEntity(sourceSystemEndpointDTO);
            existing.endpointPath = updated.endpointPath;
            existing.httpRequestType = updated.httpRequestType;
            existing.requestBodySchema = updated.requestBodySchema;
            existing.responseBodySchema = updated.responseBodySchema;
            existing.description = updated.description;
            
            // Generate TypeScript if responseBodySchema is present
            if (existing.responseBodySchema != null && !existing.responseBodySchema.isBlank()) {
                try {
                    existing.responseDts = typeScriptTypeGenerator.generate(existing.responseBodySchema);
                } catch (JsonProcessingException e) {
                    Log.warnf(e, "Could not generate TypeScript for endpoint %s", existing.endpointPath);
                    existing.responseDts = null;
                }
            } else {
                // Clear responseDts if no responseBodySchema
                existing.responseDts = null;
            }
            
            // ggf. weitere Felder übernehmen
            existing.sourceSystem = sourceSystem;
            existing.syncSystem = sourceSystem;
            existing.persist();
            return existing;
        } else {
            // Neu anlegen
            SourceSystemEndpoint sourceSystemEndpoint = sourceSystemEndpointFullMapper.mapToEntity(sourceSystemEndpointDTO);
            sourceSystemEndpoint.requestBodySchema = sourceSystemEndpointDTO.requestBodySchema();
            sourceSystemEndpoint.responseBodySchema = sourceSystemEndpointDTO.responseBodySchema();
            
            // Generate TypeScript if responseBodySchema is present
            if (sourceSystemEndpoint.responseBodySchema != null && !sourceSystemEndpoint.responseBodySchema.isBlank()) {
                try {
                    sourceSystemEndpoint.responseDts = typeScriptTypeGenerator.generate(sourceSystemEndpoint.responseBodySchema);
                } catch (JsonProcessingException e) {
                    Log.warnf(e, "Could not generate TypeScript for new endpoint %s", sourceSystemEndpoint.endpointPath);
                    sourceSystemEndpoint.responseDts = null;
                }
            } else {
                // Clear responseDts if no responseBodySchema
                sourceSystemEndpoint.responseDts = null;
            }
            
            sourceSystemEndpoint.sourceSystem = sourceSystem;
            sourceSystemEndpoint.syncSystem = sourceSystem;
            sourceSystemEndpoint.persist();
            return sourceSystemEndpoint;
        }
    }

    public List<SourceSystemEndpoint> persistSourceSystemEndpointList(@NotNull @Valid List<CreateSourceSystemEndpointDTO> endpoints, Long sourceSystemId) {
        Log.debug("Persisting source-system-endpoints for source-system with id: " + sourceSystemId);
        return endpoints.stream().map(endpointDTO -> this.persistSourceSystemEndpoint(endpointDTO, sourceSystemId)).collect(Collectors.toList());
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemEndpoint> findAllEndpointsWithSourceSystemIdLike(Long sourceSystemId) {
        Log.debugf("Finding all endpoints of source system with id = %s", sourceSystemId);
        return Optional.ofNullable(SourceSystemEndpoint.findBySourceSystemId(sourceSystemId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemEndpoint> findAllSourceSystemEndpoints() {
        Log.debug("Getting all source-system-endpoints");
        return Optional.ofNullable(SourceSystemEndpoint.<SourceSystemEndpoint>listAll())
                .orElseGet(List::of);
    }


    @Transactional(SUPPORTS)
    public Optional<SourceSystemEndpoint> findSourceSystemEndpointById(Long id) {
        Log.debugf("Finding source-system-endpoint by id = %d", id);
        return SourceSystemEndpoint.findByIdOptional(id);
    }

    public void deleteSourceSystemEndpointById(Long id) {
        Log.debugf("Deleting source-system-endpoint by id = %d", id);
        
        // First find the endpoint to ensure it exists and handle relationships properly
        Optional<SourceSystemEndpoint> endpointOpt = SourceSystemEndpoint.findByIdOptional(id);
        if (endpointOpt.isPresent()) {
            SourceSystemEndpoint endpoint = endpointOpt.get();
            
            // Clear the relationship with SourceSystem
            if (endpoint.sourceSystem != null) {
                // Remove this endpoint from the sourceSystem's sourceSystemEndpoints collection
                if (endpoint.sourceSystem.sourceSystemEndpoints != null) {
                    endpoint.sourceSystem.sourceSystemEndpoints.remove(endpoint);
                }
                endpoint.sourceSystem = null;
            }
            
            // Delete the endpoint
            endpoint.delete();
        } else {
            Log.warnf("No source-system-endpoint found with id %d", id);
        }
    }

    public Optional<SourceSystemEndpoint> replaceSourceSystemEndpoint(@NotNull @Valid SourceSystemEndpoint sourceSystemEndpoint) {
        System.out.println("### TEST replace: " + sourceSystemEndpoint.requestBodySchema);
        Log.debug("Replacing endpoint: " + sourceSystemEndpoint);

      
        if (sourceSystemEndpoint.requestBodySchema != null && !sourceSystemEndpoint.requestBodySchema.isBlank()) {
            try {
                objectMapper.readTree(sourceSystemEndpoint.requestBodySchema);
            } catch (JsonProcessingException e) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid JSON in requestBodySchema", "Das Feld requestBodySchema enthält kein valides JSON: %s", e.getMessage());
            }
        }

        if (sourceSystemEndpoint.responseBodySchema != null && !sourceSystemEndpoint.responseBodySchema.isBlank()) {
            try {
                objectMapper.readTree(sourceSystemEndpoint.responseBodySchema);
            } catch (JsonProcessingException e) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid JSON in responseBodySchema", "Das Feld responseBodySchema enthält kein valides JSON: %s", e.getMessage());
            }
        }

        return SourceSystemEndpoint.findByIdOptional(sourceSystemEndpoint.id)
                .map(SourceSystemEndpoint.class::cast) // Only here for type erasure within the IDE
                .map(targetSouceSystemEndpoint -> {
                    this.sourceSystemEndpointFullMapper.mapFullUpdate(sourceSystemEndpoint, targetSouceSystemEndpoint);
                    // Explizit das Schema übernehmen
                    targetSouceSystemEndpoint.requestBodySchema = sourceSystemEndpoint.requestBodySchema;
                    targetSouceSystemEndpoint.responseBodySchema = sourceSystemEndpoint.responseBodySchema;
                    
                    // Generate TypeScript if responseBodySchema is present
                    if (targetSouceSystemEndpoint.responseBodySchema != null && !targetSouceSystemEndpoint.responseBodySchema.isBlank()) {
                        try {
                            targetSouceSystemEndpoint.responseDts = typeScriptTypeGenerator.generate(targetSouceSystemEndpoint.responseBodySchema);
                        } catch (JsonProcessingException e) {
                            Log.warnf(e, "Could not generate TypeScript for endpoint %s (replace)", targetSouceSystemEndpoint.endpointPath);
                            targetSouceSystemEndpoint.responseDts = null;
                        }
                    } else {
                        // Clear responseDts if no responseBodySchema
                        targetSouceSystemEndpoint.responseDts = null;
                    }
                    
                    System.out.println("### TEST replace (entity): " + targetSouceSystemEndpoint.requestBodySchema);
                    Log.info("Wird gespeichert (replace): " + targetSouceSystemEndpoint.requestBodySchema);
                    return targetSouceSystemEndpoint;
                });
    }

    /**
     * Updates all existing endpoints to generate TypeScript types if they have responseBodySchema but no responseDts
     * This method should be called once after deployment to migrate existing data
     */
    @Transactional(REQUIRED)
    public void updateExistingEndpointsWithTypeScript() {
        Log.info("Starting TypeScript generation for existing endpoints...");
        
        List<SourceSystemEndpoint> allEndpoints = SourceSystemEndpoint.<SourceSystemEndpoint>listAll();
        int updatedCount = 0;
        int errorCount = 0;
        
        for (SourceSystemEndpoint endpoint : allEndpoints) {
            if (endpoint.responseBodySchema != null && !endpoint.responseBodySchema.isBlank() && 
                (endpoint.responseDts == null || endpoint.responseDts.isBlank())) {
                try {
                    endpoint.responseDts = typeScriptTypeGenerator.generate(endpoint.responseBodySchema);
                    endpoint.persist();
                    updatedCount++;
                    Log.debugf("Generated TypeScript for endpoint: %s", endpoint.endpointPath);
                } catch (JsonProcessingException e) {
                    errorCount++;
                    Log.warnf(e, "Could not generate TypeScript for existing endpoint %s", endpoint.endpointPath);
                }
            }
        }
        
        Log.infof("TypeScript generation completed. Updated: %d, Errors: %d", updatedCount, errorCount);
    }

}
