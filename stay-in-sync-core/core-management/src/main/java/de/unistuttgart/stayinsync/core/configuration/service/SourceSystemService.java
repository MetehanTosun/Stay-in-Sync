package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.SyncSystemAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemVariable;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SourceSystemService {
    @Inject
    SourceSystemFullUpdateMapper mapper;

    @Inject
    OpenApiSpecificationParserService openApiSpecificationParserService;

    public List<SourceSystem> findAllSourceSystems() {
        Log.debug("Fetching all source systems");
        return SourceSystem.listAll(); // Panache
    }

    public Optional<SourceSystem> findSourceSystemById(Long id) {
        Log.debugf("Fetching source system with ID: %d", id);
        return SourceSystem.findByIdOptional(id);
    }

    @Transactional
    public SourceSystem createSourceSystem(CreateSourceSystemDTO sourceSystemDTO) {
        /*
         * TODO: Validation logic, as soon as we know how the final Model of a
         * SourceSystem looks like.
         */
        Log.debugf("Creating new source system with name: %s", sourceSystemDTO.name());
        SourceSystem sourceSystem = mapper.mapToEntity(sourceSystemDTO);

        if (sourceSystemDTO.openApiSpec() != null && !sourceSystemDTO.openApiSpec().isBlank()) {
            sourceSystem.openApiSpec = sourceSystemDTO.openApiSpec();
        }

        sourceSystem.persist();
        openApiSpecificationParserService.synchronizeFromSpec(sourceSystem);
        return sourceSystem;
    }

    @Transactional
    public Optional<SourceSystem> updateSourceSystem(CreateSourceSystemDTO sourceSystemDTO) {
        Log.debugf("Updating source system with ID: %d", sourceSystemDTO.id());
        SourceSystem existingSs = SourceSystem.findById(sourceSystemDTO.id());
        if (existingSs != null) {
            mapper.mapFullUpdate(mapper.mapToEntity(sourceSystemDTO), existingSs);
            openApiSpecificationParserService.synchronizeFromSpec(existingSs);
        }
        return Optional.ofNullable(existingSs);
    }

    @Transactional(rollbackOn = Exception.class)
    public boolean deleteSourceSystemById(Long id) {
        Log.debugf("Deleting source system with ID: %d", id);
        
        // First find the source system to ensure it exists
        Optional<SourceSystem> sourceSystemOpt = SourceSystem.findByIdOptional(id);
        if (sourceSystemOpt.isPresent()) {
            SourceSystem sourceSystem = sourceSystemOpt.get();
            
            // Delete related entities first using proper cascade deletion
            try {
                Log.debugf("Starting deletion of related entities for source system ID: %d", id);
                
                // Delete source system API request configurations using native SQL to avoid inheritance issues
                long deletedConfigs = SourceSystemApiRequestConfiguration.delete("sourceSystem.id", id);
                Log.debugf("Deleted %d API request configurations", deletedConfigs);
                
                // Delete source system endpoints and their related entities
                List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(id);
                Log.debugf("Found %d endpoints to delete", endpoints.size());
                for (SourceSystemEndpoint endpoint : endpoints) {
                    // Delete source system variables for this endpoint
                    List<SourceSystemVariable> variables = SourceSystemVariable.list("sourceSystemEndpoint.id", endpoint.id);
                    Log.debugf("Found %d source system variables to delete for endpoint ID: %d", variables.size(), endpoint.id);
                    for (SourceSystemVariable variable : variables) {
                        variable.delete();
                    }
                    
                    // Delete query param values for this endpoint
                    List<ApiEndpointQueryParamValue> queryParamValues = ApiEndpointQueryParamValue.list("queryParam.syncSystemEndpoint.id", endpoint.id);
                    Log.debugf("Found %d query param values to delete for endpoint ID: %d", queryParamValues.size(), endpoint.id);
                    for (ApiEndpointQueryParamValue value : queryParamValues) {
                        value.delete();
                    }
                    
                    // Delete query params for this endpoint
                    List<ApiEndpointQueryParam> queryParams = ApiEndpointQueryParam.findByEndpointId(endpoint.id);
                    Log.debugf("Found %d query params to delete for endpoint ID: %d", queryParams.size(), endpoint.id);
                    for (ApiEndpointQueryParam param : queryParams) {
                        param.delete();
                    }
                    
                    // Delete the endpoint itself (this will handle the inheritance properly)
                    endpoint.delete();
                }
                
                // Delete API headers using native SQL to avoid inheritance issues
                long deletedHeaders = ApiHeader.delete("syncSystem.id", id);
                Log.debugf("Deleted %d API headers", deletedHeaders);
                
                // Delete auth config if exists
                List<SyncSystemAuthConfig> authConfigs = SyncSystemAuthConfig.list("syncSystem.id", id);
                Log.debugf("Found %d auth configs to delete", authConfigs.size());
                for (SyncSystemAuthConfig authConfig : authConfigs) {
                    authConfig.delete();
                }
                
                // Finally delete the source system itself
                sourceSystem.delete();
                
                Log.debugf("Successfully deleted source system with ID: %d", id);
                return true;
            } catch (Exception e) {
                Log.errorf(e, "Error deleting source system with ID: %d. Transaction will be rolled back.", id);
                // The transaction will be automatically rolled back due to @Transactional(rollbackOn = Exception.class)
                // Re-throw as a CoreManagementException with proper error details
                throw new CoreManagementException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Failed to delete source system",
                    "Error deleting source system with ID %d: %s", id, e.getMessage()
                );
            }
        } else {
            Log.warnf("No source system found with id %d", id);
            return false;
        }
    }
}