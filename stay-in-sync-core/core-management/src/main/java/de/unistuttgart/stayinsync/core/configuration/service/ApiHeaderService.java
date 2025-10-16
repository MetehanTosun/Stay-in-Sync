package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiHeaderValue;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiHeaderFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiHeaderDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateApiHeaderDTO;
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

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class ApiHeaderService {

    @Inject
    Validator validator;

    @Inject
    SyncSystemService syncSystemService;

    @Inject
    ApiHeaderFullUpdateMapper mapper;

    public ApiHeader persistRequestHeader(@NotNull @Valid CreateApiHeaderDTO apiEndpointQueryParamDTO, Long endpointId) {
        Log.debugf("Persisting api-endpoint-query-param: %s, for sync-syste, with id: %s", apiEndpointQueryParamDTO, endpointId);

        ApiHeader apiRequestHeader = mapper.mapToEntity(apiEndpointQueryParamDTO);

        SyncSystem syncSystem = syncSystemService.findSyncSystemById(endpointId).orElseThrow(() -> {
            return new CoreManagementException("Unable to find Endpoint", "There is no endpoint with id %s", endpointId);
        });
        apiRequestHeader.syncSystem = syncSystem;
        apiRequestHeader.persist();

        return apiRequestHeader;
    }

    @Transactional(SUPPORTS)
    public List<ApiHeader> findAllHeadersBySyncSystemId(Long syncSystemId) {
        Log.debugf("Finding all endpoints of sync-syste, with id = %s", syncSystemId);
        return Optional.ofNullable(ApiHeader.findBySyncSystemId(syncSystemId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public Optional<ApiHeader> findRequestHeaderById(Long id) {
        Log.debugf("Finding api-request-header by id = %d", id);
        
        List<ApiHeader> headers = ApiHeader.find("id", id).list();
        return headers.isEmpty() ? Optional.empty() : Optional.of(headers.get(0));
    }

    public void deleteRequestHeaderById(Long id) {
        Log.debugf("Deleting api-request-header by id = %d", id);
        
        try {
            
            long deletedValues = ApiHeaderValue.delete("apiHeader.id", id);
            Log.debugf("Deleted %d header values for header ID: %d", deletedValues, id);
            
            long deletedHeaders = ApiHeader.delete("id", id);
            Log.debugf("Deleted %d headers with ID: %d", deletedHeaders, id);
            
            if (deletedHeaders == 0) {
                Log.warnf("No api-request-header found with id %d", id);
            } else {
                Log.debugf("Successfully deleted api-request-header with ID: %d", id);
            }
        } catch (Exception e) {
            Log.errorf(e, "Error deleting api-request-header with ID: %d", id);
            throw new CoreManagementException(
                Response.Status.INTERNAL_SERVER_ERROR,
                "Failed to delete API header",
                "Error deleting API header with ID %d: %s", id, e.getMessage()
            );
        }
    }

    public Optional<ApiHeader> replaceRequestHeader(@NotNull @Valid ApiHeaderDTO apiEndpointQueryParamDTO) {
        ApiHeader apiEndpointQueryParam = mapper.mapToEntity(apiEndpointQueryParamDTO);
        Log.debugf("Replacing endpoint: %s", apiEndpointQueryParam);

        Optional<ApiHeader> updatedSourceSystemEndpoint = ApiHeader.findByIdOptional(apiEndpointQueryParam.id)
                .map(ApiHeader.class::cast) 
                .map(targetSouceSystemEndpoint -> {
                    this.mapper.mapFullUpdate(apiEndpointQueryParam, targetSouceSystemEndpoint);
                    return targetSouceSystemEndpoint;
                });

        return updatedSourceSystemEndpoint;
    }
}
