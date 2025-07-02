package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeaderValue;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiHeaderValueMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiHeaderValueDTO;
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
public class ApiHeaderValueService {

    @Inject
    Validator validator;

    @Inject
    SyncSystemService syncSystemService;

    @Inject
    ApiRequestConfigurationService apiRequestConfigurationService;

    @Inject
    ApiHeaderValueMapper mapper;

    public ApiHeaderValue persistHeaderValue(@NotNull @Valid ApiHeaderValueDTO apiHeaderValueDTO, Long requestConfigurationId) {
        Log.debugf("Persisting api-endpoint-query-param: %s, for source-system with id: %s", apiHeaderValueDTO, requestConfigurationId);

        ApiHeaderValue apiHeaderValue = mapper.mapToEntity(apiHeaderValueDTO);

        ApiRequestConfiguration requestConfiguration = apiRequestConfigurationService.findApiRequestConfigurationById(requestConfigurationId).orElseThrow(() -> {
            return new CoreManagementException("Unable to find request configuration", "There is no request configuration with id %s", requestConfigurationId);
        });
        apiHeaderValue.requestConfiguration = requestConfiguration;
        apiHeaderValue.persist();

        return apiHeaderValue;
    }

    @Transactional(SUPPORTS)
    public List<ApiHeaderValue> findByRequestConfigurationId(Long endpointId) {
        Log.debugf("Finding all endpoints of source system with id = %s", endpointId);
        return Optional.ofNullable(ApiHeaderValue.findRequestHeadersByConfigurationId(endpointId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public Optional<ApiHeaderValue> findHeaderValueById(Long id) {
        Log.debugf("Finding api-header-value by id = %d", id);
        return ApiHeaderValue.findByIdOptional(id);
    }

    public void deleteApiHeaderValueById(Long id) {
        Log.debugf("Deleting endpoint by id = %d", id);
        ApiHeaderValue.deleteById(id);
    }

    public Optional<ApiHeaderValue> replaceHeaderValue(@NotNull @Valid ApiHeaderValueDTO apiHeaderValueDTO) {
        ApiHeaderValue updatedHeaderRequestConfiguration = mapper.mapToEntity(apiHeaderValueDTO);
        Log.debugf("Replacing endpoint: %s", updatedHeaderRequestConfiguration);

        Optional<ApiHeaderValue> updatedHeaderValue = ApiHeaderValue.findByIdOptional(updatedHeaderRequestConfiguration.id)
                .map(ApiHeaderValue.class::cast) // Only here for type erasure within the IDE
                .map(targetApiHeaderValue -> {
                    this.mapper.mapFullUpdate(updatedHeaderRequestConfiguration, targetApiHeaderValue);
                    return targetApiHeaderValue;
                });

        return updatedHeaderValue;
    }
}
