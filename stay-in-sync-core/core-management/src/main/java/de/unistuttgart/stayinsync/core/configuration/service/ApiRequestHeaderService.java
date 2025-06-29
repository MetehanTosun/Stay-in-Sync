package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiHeaderFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiRequestHeaderDTO;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class ApiRequestHeaderService {

    @Inject
    Validator validator;

    @Inject
    SyncSystemService syncSystemService;

    @Inject
    ApiHeaderFullUpdateMapper mapper;

    public ApiHeader persistRequestHeader(@NotNull @Valid ApiRequestHeaderDTO apiEndpointQueryParamDTO, Long endpointId) {
        Log.debugf("Persisting api-endpoint-query-param: %s, for source-system with id: %s", apiEndpointQueryParamDTO, endpointId);

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
        Log.debugf("Finding all endpoints of source system with id = %s", syncSystemId);
        return Optional.ofNullable(ApiHeader.findBySyncSystemId(syncSystemId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public Optional<ApiHeader> findRequestHeaderById(Long id) {
        Log.debugf("Finding api-endpoint-query-param by id = %d", id);
        return ApiHeader.findByIdOptional(id);
    }

    public void deleteRequestHeaderById(Long id) {
        Log.debugf("Deleting endpoint by id = %d", id);
        ApiHeader.deleteById(id);
    }

    public Optional<ApiHeader> replaceRequestHeader(@NotNull @Valid ApiRequestHeaderDTO apiEndpointQueryParamDTO) {
        ApiHeader apiEndpointQueryParam = mapper.mapToEntity(apiEndpointQueryParamDTO);
        Log.debugf("Replacing endpoint: %s", apiEndpointQueryParam);

        Optional<ApiHeader> updatedSourceSystemEndpoint = ApiHeader.findByIdOptional(apiEndpointQueryParam.id)
                .map(ApiHeader.class::cast) // Only here for type erasure within the IDE
                .map(targetSouceSystemEndpoint -> {
                    this.mapper.mapFullUpdate(apiEndpointQueryParam, targetSouceSystemEndpoint);
                    return targetSouceSystemEndpoint;
                });

        return updatedSourceSystemEndpoint;
    }
}
