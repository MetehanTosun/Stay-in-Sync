package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncSystem;
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
        Log.infof("Persisting api-endpoint-query-param: %s, for sync-syste, with id: %s", apiEndpointQueryParamDTO, endpointId);

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
        Log.infof("Finding all endpoints of sync-syste, with id = %s", syncSystemId);
        return Optional.ofNullable(ApiHeader.findBySyncSystemId(syncSystemId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public Optional<ApiHeader> findRequestHeaderById(Long id) {
        Log.infof("Finding api-endpoint-query-param by id = %d", id);
        return ApiHeader.findByIdOptional(id);
    }

    public void deleteRequestHeaderById(Long id) {
        Log.infof("Deleting endpoint by id = %d", id);
        ApiHeader.deleteById(id);
    }

    public Optional<ApiHeader> replaceRequestHeader(@NotNull @Valid ApiHeaderDTO apiEndpointQueryParamDTO) {
        ApiHeader apiEndpointQueryParam = mapper.mapToEntity(apiEndpointQueryParamDTO);
        Log.infof("Replacing endpoint: %s", apiEndpointQueryParam);

        Optional<ApiHeader> updatedSourceSystemEndpoint = ApiHeader.findByIdOptional(apiEndpointQueryParam.id)
                .map(ApiHeader.class::cast) // Only here for type erasure within the IDE
                .map(targetSouceSystemEndpoint -> {
                    this.mapper.mapFullUpdate(apiEndpointQueryParam, targetSouceSystemEndpoint);
                    return targetSouceSystemEndpoint;
                });

        return updatedSourceSystemEndpoint;
    }
}
