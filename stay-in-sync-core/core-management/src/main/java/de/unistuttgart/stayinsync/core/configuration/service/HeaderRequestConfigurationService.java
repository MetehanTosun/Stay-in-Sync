package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfigurationHeader;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiHeaderFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.HeaderRequestConfigurationDTO;
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
public class HeaderRequestConfigurationService {

    @Inject
    Validator validator;

    @Inject
    SyncSystemService syncSystemService;

    @Inject
    ApiRequestConfigurationService apiRequestConfigurationService;

    @Inject
    ApiHeaderFullUpdateMapper mapper;

    public ApiRequestConfigurationHeader persistHeaderRequestConfiguration(@NotNull @Valid HeaderRequestConfigurationDTO headerRequestConfigurationDTO, Long requestConfigurationId) {
        Log.debugf("Persisting api-endpoint-query-param: %s, for source-system with id: %s", headerRequestConfigurationDTO, requestConfigurationId);

        ApiRequestConfigurationHeader apiEndpointQueryParam = mapper.mapToEntity(headerRequestConfigurationDTO);

        ApiRequestConfiguration requestConfiguration = apiRequestConfigurationService.findApiRequestConfigurationById(requestConfigurationId).orElseThrow(() -> {
            return new CoreManagementException("Unable to find Endpoint", "There is no endpoint with id %s", requestConfigurationId);
        });
        apiEndpointQueryParam.requestConfiguration = requestConfiguration;
        apiEndpointQueryParam.persist();

        return apiEndpointQueryParam;
    }

    @Transactional(SUPPORTS)
    public List<ApiEndpointQueryParam> findAllQueryParamsByEndpointId(Long endpointId) {
        Log.debugf("Finding all endpoints of source system with id = %s", endpointId);
        return Optional.ofNullable(ApiEndpointQueryParam.findByEndpointId(endpointId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public Optional<ApiEndpointQueryParam> findQueryParamById(Long id) {
        Log.debugf("Finding api-endpoint-query-param by id = %d", id);
        return ApiEndpointQueryParam.findByIdOptional(id);
    }

    public void deleteQueryParamById(Long id) {
        Log.debugf("Deleting endpoint by id = %d", id);
        ApiEndpointQueryParam.deleteById(id);
    }

    public Optional<ApiRequestConfigurationHeader> replaceQueryParamById(@NotNull @Valid HeaderRequestConfigurationDTO headerRequestConfigurationDTO) {
        ApiRequestConfigurationHeader updatedHeaderRequestConfiguration = mapper.mapToEntity(headerRequestConfigurationDTO);
        Log.debugf("Replacing endpoint: %s", updatedHeaderRequestConfiguration);

        Optional<ApiRequestConfigurationHeader> updatedRequestHeaderConfiguration = ApiRequestConfigurationHeader.findByIdOptional(updatedHeaderRequestConfiguration.id)
                .map(ApiRequestConfigurationHeader.class::cast) // Only here for type erasure within the IDE
                .map(targetSouceSystemEndpoint -> {
                    this.mapper.mapFullUpdate(updatedHeaderRequestConfiguration, targetSouceSystemEndpoint);
                    return targetSouceSystemEndpoint;
                });

        return updatedRequestHeaderConfiguration;
    }
}
