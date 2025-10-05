package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiEndpointQueryParamValueMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamValueDTO;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.util.List;
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

    public ApiEndpointQueryParamValue persistValue(@NotNull @Valid ApiEndpointQueryParamValueDTO queryParamConfigurationDTO, Long requestConfigurationId) {
        Log.debugf("Persisting api-endpoint-query-param: %s, for source-system with id: %s", queryParamConfigurationDTO, requestConfigurationId);

        ApiEndpointQueryParam queryParam = ApiEndpointQueryParam.findById(queryParamConfigurationDTO.queryParamId());
        ApiEndpointQueryParamValue paramValue = mapper.mapToEntity(queryParamConfigurationDTO);

        ApiRequestConfiguration requestConfiguration = apiRequestConfigurationService.findApiRequestConfigurationById(requestConfigurationId).orElseThrow(() -> {
            return new CoreManagementException("Unable to find Endpoint", "There is no endpoint with id %s", requestConfigurationId);
        });

        paramValue.requestConfiguration = requestConfiguration;
        paramValue.persist();
        queryParam.values.add(paramValue.selectedValue);

        return paramValue;
    }

    @Transactional(SUPPORTS)
    public List<ApiEndpointQueryParamValue> findQueryParamValueByRequestConfig(Long requestConfigId) {
        Log.debugf("Finding api-endpoint-query-param by request config id = %d", requestConfigId);
        return ApiEndpointQueryParamValue.findRequestHeadersByConfigurationId(requestConfigId);
    }


    @Transactional(SUPPORTS)
    public Optional<ApiEndpointQueryParamValue> findQueryParamValueById(Long id) {
        Log.debugf("Finding api-endpoint-query-param by id = %d", id);
        return ApiEndpointQueryParamValue.findByIdOptional(id);
    }

    public void deleteQueryParamValue(Long id) {
        Log.debugf("Deleting endpoint by id = %d", id);
        ApiEndpointQueryParamValue.deleteById(id);
    }

    public Optional<ApiEndpointQueryParamValue> replaceConfiguration(@NotNull @Valid ApiEndpointQueryParamValueDTO queryParamConfiguration) {
        ApiEndpointQueryParamValue apiEndpointQueryParam = mapper.mapToEntity(queryParamConfiguration);
        Log.debugf("Replacing endpoint: %s", apiEndpointQueryParam);

        Optional<ApiEndpointQueryParamValue> updatedQueryParamValue = apiEndpointQueryParam.findByIdOptional(apiEndpointQueryParam.id)
                .map(ApiEndpointQueryParamValue.class::cast)
                .map(targetQueryParamValue -> {
                    this.mapper.mapFullUpdate(apiEndpointQueryParam, targetQueryParamValue);
                    return targetQueryParamValue;
                });

        return updatedQueryParamValue;
    }
}
