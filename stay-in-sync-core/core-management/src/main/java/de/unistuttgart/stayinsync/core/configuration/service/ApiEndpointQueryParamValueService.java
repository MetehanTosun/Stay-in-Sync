package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiEndpointQueryParamValueMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpoindQueryParamValueDTO;
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
public class ApiEndpointQueryParamValueService {

    @Inject
    ApiEndpointQueryParamValueMapper mapper;

    @Inject
    ApiRequestConfigurationService apiRequestConfigurationService;

    public ApiEndpointQueryParamValue persistConfiguration(@NotNull @Valid ApiEndpoindQueryParamValueDTO queryParamConfigurationDTO, Long requestConfigurationId) {
        Log.debugf("Persisting api-endpoint-query-param: %s, for source-system with id: %s", queryParamConfigurationDTO, requestConfigurationId);

        de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue apiEndpointQueryParam = mapper.mapToEntity(queryParamConfigurationDTO);

        ApiRequestConfiguration requestConfiguration = apiRequestConfigurationService.findApiRequestConfigurationById(requestConfigurationId).orElseThrow(() -> {
            return new CoreManagementException("Unable to find Endpoint", "There is no endpoint with id %s", requestConfigurationId);
        });
        apiEndpointQueryParam.requestConfiguration = requestConfiguration;
        apiEndpointQueryParam.persist();

        return apiEndpointQueryParam;
    }


    @Transactional(SUPPORTS)
    public Optional<ApiEndpointQueryParam> findQueryParamValueById(Long id) {
        Log.debugf("Finding api-endpoint-query-param by id = %d", id);
        return ApiEndpointQueryParam.findByIdOptional(id);
    }

    public void deleteQueryParamValue(Long id) {
        Log.debugf("Deleting endpoint by id = %d", id);
        ApiEndpointQueryParam.deleteById(id);
    }

    public Optional<de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue> replaceConfiguration(@NotNull @Valid ApiEndpoindQueryParamValueDTO queryParamConfiguration) {
        de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue apiEndpointQueryParam = mapper.mapToEntity(queryParamConfiguration);
        Log.debugf("Replacing endpoint: %s", apiEndpointQueryParam);

        Optional<de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue> updatedSourceSystemEndpoint = apiEndpointQueryParam.findByIdOptional(apiEndpointQueryParam.id)
                .map(de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue.class::cast) // Only here for type erasure within the IDE
                .map(targetSouceSystemEndpoint -> {
                    this.mapper.mapFullUpdate(apiEndpointQueryParam, targetSouceSystemEndpoint);
                    return targetSouceSystemEndpoint;
                });

        return updatedSourceSystemEndpoint;
    }
}
