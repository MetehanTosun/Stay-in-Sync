package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig.SyncSystemAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystemVariable;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiEndpointQueryParamValue;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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
        return SourceSystem.listAll(); 
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
        
        
        Optional<SourceSystem> sourceSystemOpt = SourceSystem.findByIdOptional(id);
        if (sourceSystemOpt.isPresent()) {
            SourceSystem sourceSystem = sourceSystemOpt.get();
            
            
            try {
                Log.debugf("Starting deletion of related entities for source system ID: %d", id);
                
                // Cleanup AAS snapshot lites (elements first, then submodels)
                long deletedElements = AasElementLite.delete("submodelLite.sourceSystem.id", id);
                Log.debugf("Deleted %d AAS element lites", deletedElements);
                long deletedSubmodels = AasSubmodelLite.delete("sourceSystem.id", id);
                Log.debugf("Deleted %d AAS submodel lites", deletedSubmodels);

                
                long deletedConfigs = SourceSystemApiRequestConfiguration.delete("sourceSystem.id", id);
                Log.debugf("Deleted %d API request configurations", deletedConfigs);
                
          
                List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(id);
                Log.debugf("Found %d endpoints to delete", endpoints.size());
                for (SourceSystemEndpoint endpoint : endpoints) {
                   
                    List<SourceSystemVariable> variables = SourceSystemVariable.list("sourceSystemEndpoint.id", endpoint.id);
                    Log.debugf("Found %d source system variables to delete for endpoint ID: %d", variables.size(), endpoint.id);
                    for (SourceSystemVariable variable : variables) {
                        variable.delete();
                    }
                    
                 
                    List<ApiEndpointQueryParamValue> queryParamValues = ApiEndpointQueryParamValue.list("queryParam.syncSystemEndpoint.id", endpoint.id);
                    Log.debugf("Found %d query param values to delete for endpoint ID: %d", queryParamValues.size(), endpoint.id);
                    for (ApiEndpointQueryParamValue value : queryParamValues) {
                        value.delete();
                    }
                    
                 
                    List<ApiEndpointQueryParam> queryParams = ApiEndpointQueryParam.findByEndpointId(endpoint.id);
                    Log.debugf("Found %d query params to delete for endpoint ID: %d", queryParams.size(), endpoint.id);
                    for (ApiEndpointQueryParam param : queryParams) {
                        param.delete();
                    }
                    
            
                    endpoint.delete();
                }
                
                
                long deletedHeaders = ApiHeader.delete("syncSystem.id", id);
                Log.debugf("Deleted %d API headers", deletedHeaders);
                
               
                List<SyncSystemAuthConfig> authConfigs = SyncSystemAuthConfig.list("syncSystem.id", id);
                Log.debugf("Found %d auth configs to delete", authConfigs.size());
                for (SyncSystemAuthConfig authConfig : authConfigs) {
                    authConfig.delete();
                }
                
               
                sourceSystem.delete();
                
                Log.debugf("Successfully deleted source system with ID: %d", id);
                return true;
            } catch (Exception e) {
                Log.errorf(e, "Error deleting source system with ID: %d. Transaction will be rolled back.", id);
             
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