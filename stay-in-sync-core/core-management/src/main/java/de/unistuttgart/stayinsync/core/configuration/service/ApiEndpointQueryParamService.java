package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiEndpointQueryParamMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamDTO;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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
public class ApiEndpointQueryParamService {

    @Inject
    Validator validator;

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    ApiEndpointQueryParamMapper mapper;

    @Inject
    EntityManager entityManager;

    public ApiEndpointQueryParam persistApiQueryParam(@NotNull @Valid ApiEndpointQueryParamDTO apiEndpointQueryParamDTO, Long endpointId) {
        Log.debugf("Persisting api-endpoint-query-param: %s, for source-system with id: %s", apiEndpointQueryParamDTO, endpointId);

        ApiEndpointQueryParam apiEndpointQueryParam = mapper.mapToEntity(apiEndpointQueryParamDTO);

        if (apiEndpointQueryParam.queryParamType != null && apiEndpointQueryParam.queryParamType.name().equals("PATH")) {
            apiEndpointQueryParam.paramName = ensureBraces(apiEndpointQueryParam.paramName);
        }

        SourceSystemEndpoint endpoint = sourceSystemEndpointService.findSourceSystemEndpointById(endpointId).orElseThrow(() -> {
            return new CoreManagementException("Unable to find Endpoint", "There is no endpoint with id %s", endpointId);
        });
        apiEndpointQueryParam.syncSystemEndpoint = endpoint;
        apiEndpointQueryParam.persist();

        return apiEndpointQueryParam;
    }

    @Transactional(SUPPORTS)
    public List<ApiEndpointQueryParam> findAllQueryParamsByEndpointId(Long endpointId) {
        Log.debugf("Finding all query params for endpoint with id = %s", endpointId);
        try {
           
            return ApiEndpointQueryParam.find("syncSystemEndpoint.id", endpointId).list();
        } catch (Exception e) {
            Log.errorf(e, "Error finding query params for endpoint ID: %d", endpointId);
            throw new CoreManagementException(
                Response.Status.INTERNAL_SERVER_ERROR,
                "Failed to find query params",
                "Error finding query params for endpoint with ID %d: %s", endpointId, e.getMessage()
            );
        }
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

    public Optional<ApiEndpointQueryParam> replaceQueryParam(@NotNull @Valid ApiEndpointQueryParamDTO apiEndpointQueryParamDTO) {
        ApiEndpointQueryParam apiEndpointQueryParam = mapper.mapToEntity(apiEndpointQueryParamDTO);
       
        if (apiEndpointQueryParam.queryParamType != null && apiEndpointQueryParam.queryParamType.name().equals("PATH")) {
            apiEndpointQueryParam.paramName = ensureBraces(apiEndpointQueryParam.paramName);
        }
        Log.debugf("Replacing endpoint: %s", apiEndpointQueryParam);

        Optional<ApiEndpointQueryParam> updatedSourceSystemEndpoint = apiEndpointQueryParam.findByIdOptional(apiEndpointQueryParam.id)
                .map(ApiEndpointQueryParam.class::cast) 
                .map(targetSouceSystemEndpoint -> {
                    this.mapper.mapFullUpdate(apiEndpointQueryParam, targetSouceSystemEndpoint);
                    return targetSouceSystemEndpoint;
                });

        return updatedSourceSystemEndpoint;
    }

    
    private String ensureBraces(String paramName) {
        if (paramName == null) return null;
        String clean = paramName.replaceAll("[{}]", "");
        return "{" + clean + "}";
    }
}
