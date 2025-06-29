package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfigurationQueryParam;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiEndpointQueryParamMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.QueryParamRequestConfigurationDTO;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class QueryParamRequestConfigurationService {

    @Inject
    ApiEndpointQueryParamMapper mapper;

    @Inject
    ApiRequestConfigurationService apiRequestConfigurationService;

    public ApiRequestConfigurationQueryParam persistConfiguration(@NotNull @Valid QueryParamRequestConfigurationDTO queryParamConfigurationDTO, Long requestConfigurationId) {
        Log.debugf("Persisting api-endpoint-query-param: %s, for source-system with id: %s", queryParamConfigurationDTO, requestConfigurationId);

        ApiRequestConfigurationQueryParam apiEndpointQueryParam = mapper.mapToEntity(queryParamConfigurationDTO);

        ApiRequestConfiguration requestConfiguration = apiRequestConfigurationService.findApiRequestConfigurationById(requestConfigurationId).orElseThrow(() -> {
            return new CoreManagementException("Unable to find Endpoint", "There is no endpoint with id %s", requestConfigurationId);
        });
        apiEndpointQueryParam.requestConfiguration = requestConfiguration;
        apiEndpointQueryParam.persist();

        return apiEndpointQueryParam;
    }


    @Transactional(SUPPORTS)
    public Optional<ApiEndpointQueryParam> findQueryParamById(Long id) {
        Log.debugf("Finding api-endpoint-query-param by id = %d", id);
        return ApiEndpointQueryParam.findByIdOptional(id);
    }

    public void deleteConfiguration(Long id) {
        Log.debugf("Deleting endpoint by id = %d", id);
        ApiEndpointQueryParam.deleteById(id);
    }

    public Optional<ApiRequestConfigurationQueryParam> replaceConfiguration(@NotNull @Valid QueryParamRequestConfigurationDTO queryParamConfiguration) {
        ApiRequestConfigurationQueryParam apiEndpointQueryParam = mapper.mapToEntity(queryParamConfiguration);
        Log.debugf("Replacing endpoint: %s", apiEndpointQueryParam);

        Optional<ApiRequestConfigurationQueryParam> updatedSourceSystemEndpoint = apiEndpointQueryParam.findByIdOptional(apiEndpointQueryParam.id)
                .map(ApiRequestConfigurationQueryParam.class::cast) // Only here for type erasure within the IDE
                .map(targetSouceSystemEndpoint -> {
                    this.mapper.mapFullUpdate(apiEndpointQueryParam, targetSouceSystemEndpoint);
                    return targetSouceSystemEndpoint;
                });

        return updatedSourceSystemEndpoint;
    }
}
