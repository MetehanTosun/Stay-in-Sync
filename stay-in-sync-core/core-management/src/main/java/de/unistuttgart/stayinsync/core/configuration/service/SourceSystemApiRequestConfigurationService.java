package de.unistuttgart.stayinsync.core.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemApiRequestConfigurationFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.util.TypeScriptTypeGenerator;
import de.unistuttgart.stayinsync.transport.domain.ApiEndpointQueryParamType;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class SourceSystemApiRequestConfigurationService {
    @Inject
    Validator validator;

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    TypeScriptTypeGenerator typeGenerator;

    @Inject
    SourceSystemApiRequestConfigurationFullUpdateMapper mapper;

    // TODO: Evaluate if method is necessary since ARC creation has cascading effects
    public SourceSystemApiRequestConfiguration persistApiRequestConfiguration(@NotNull @Valid CreateRequestConfigurationDTO sourceSystemApiRequestConfigurationDTO, Long endpointId) {
        SourceSystemApiRequestConfiguration sourceSystemApiRequestConfiguration = mapper.mapToEntity(sourceSystemApiRequestConfigurationDTO);

        Log.debugf("Persisting request-configurations: %s, for endpoint with id: %s", sourceSystemApiRequestConfiguration, endpointId);

        SourceSystemEndpoint sourceSystemEndpoint = sourceSystemEndpointService.findSourceSystemEndpointById(endpointId).orElseThrow(() ->
                new CoreManagementException("Unable to find Source System", "There is no source-system with id %s", endpointId));

        sourceSystemApiRequestConfiguration.sourceSystem = sourceSystemEndpoint.sourceSystem;
        sourceSystemApiRequestConfiguration.sourceSystemEndpoint = sourceSystemEndpoint;
        sourceSystemEndpoint.apiRequestConfigurations.add(sourceSystemApiRequestConfiguration);
        sourceSystemEndpoint.sourceSystem.sourceSystemApiRequestConfigurations.add(sourceSystemApiRequestConfiguration);
        sourceSystemApiRequestConfiguration.persist();


        return sourceSystemApiRequestConfiguration;
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemApiRequestConfiguration> findAllRequestConfigurationsWithSourceSystemIdLike(Long sourceSystemId) {
        Log.debugf("Finding all request-configurations of source system with id = %s", sourceSystemId);
        return Optional.ofNullable(SourceSystemApiRequestConfiguration.findBySourceSystemId(sourceSystemId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemApiRequestConfiguration> findAllRequestConfigurationsByEndpointId(Long endpointId) {
        Log.debugf("Finding all request-configurations of endpoint with id = %s", endpointId);
        return Optional.ofNullable(SourceSystemApiRequestConfiguration.findByEndpointId(endpointId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemApiRequestConfiguration> findAllApiRequestConfigurations() {
        Log.debug("Getting all request-configurations");
        return Optional.ofNullable(SourceSystemApiRequestConfiguration.<SourceSystemApiRequestConfiguration>listAll())
                .orElseGet(List::of);
    }


    @Transactional(SUPPORTS)
    public Optional<SourceSystemApiRequestConfiguration> findApiRequestConfigurationById(Long id) {
        Log.debugf("Finding request-configurations by id = %d", id);
        return SourceSystemApiRequestConfiguration.findByIdOptional(id);
    }

    public void deleteApiRequestConfigurationById(Long id) {
        Log.debugf("Deleting request-configurations by id = %d", id);
        SourceSystemApiRequestConfiguration.deleteById(id);
    }

    public Optional<SourceSystemApiRequestConfiguration> replaceApiRequestConfiguration(@NotNull @Valid CreateRequestConfigurationDTO sourceSystemApiRequestConfigurationDTO) {
        SourceSystemApiRequestConfiguration sourceSystemApiRequestConfiguration = mapper.mapToEntity(sourceSystemApiRequestConfigurationDTO);
        Log.debugf("Replacing request-configurations: %s", sourceSystemApiRequestConfiguration);

        return SourceSystemApiRequestConfiguration.findByIdOptional(sourceSystemApiRequestConfiguration.id)
                .map(SourceSystemApiRequestConfiguration.class::cast) // Only here for type erasure within the IDE
                .map(targetRequestConfiguration -> {
                    this.mapper.mapFullUpdate(sourceSystemApiRequestConfiguration, targetRequestConfiguration);
                    return targetRequestConfiguration;
                });
    }

    @Transactional
    public SourceSystemApiRequestConfiguration create(CreateArcDTO dto, Long endpointId) {
        SourceSystem sourceSystem = SourceSystem.<SourceSystem>findByIdOptional(dto.sourceSystemId())
                .orElseThrow(() -> new NotFoundException("SourceSystem not found."));
        SourceSystemEndpoint endpoint = SourceSystemEndpoint.<SourceSystemEndpoint>findByIdOptional(endpointId)
                .orElseThrow(() -> new NotFoundException("SourceSystemEndpoint not found."));

        if (!endpoint.sourceSystem.id.equals(sourceSystem.id)) {
            throw new CoreManagementException("Source System mismatch","Endpoint does not belong to the specified source system.");
        }

        String generatedDts;
        try {
            generatedDts = typeGenerator.generate(dto.responseDts());
        } catch (JsonProcessingException e) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid sample JSON", "The provided JSON is invalid and cannot be built into a TypeScript type: %s", e.getMessage());
        }

        SourceSystemApiRequestConfiguration newArc = mapper.mapToEntity(dto);

        newArc.sourceSystem = sourceSystem;
        newArc.sourceSystemEndpoint = endpoint;
        newArc.syncSystemEndpoint = endpoint;
        newArc.responseDts = generatedDts;

        newArc.persist();

        createAndPersistParameterValues(dto.pathParameterValues(), newArc, endpoint, ApiEndpointQueryParamType.PATH);
        createAndPersistParameterValues(dto.queryParameterValues(), newArc, endpoint, ApiEndpointQueryParamType.QUERY);
        createAndPersistHeaderValues(dto.headerValues(), newArc, sourceSystem);

        return newArc;
    }

    /**
     * A unified method to find a parameter definition and persist its configured value.
     * It uses the parameter type (PATH or QUERY) to find the correct definition.
     */
    private void createAndPersistParameterValues(
            Map<String, String> values,
            ApiRequestConfiguration arc,
            SyncSystemEndpoint endpoint,
            ApiEndpointQueryParamType type) {

        if (values == null) return;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();

            ApiEndpointQueryParam paramDefinition =ApiEndpointQueryParam
                    .find("paramName = ?1 and syncSystemEndpoint = ?2 and queryParamType = ?3",
                            paramName, endpoint, type)
                    .firstResultOptional()
                    .map(ApiEndpointQueryParam.class::cast) // IDE type erasure
                    .orElseThrow(() -> new CoreManagementException("Parameter not defined",
                            type + " parameter '" + paramName + "' is not defined for this endpoint."));

            ApiEndpointQueryParamValue parameterValue = new ApiEndpointQueryParamValue();
            parameterValue.requestConfiguration = arc;
            parameterValue.queryParam = paramDefinition;
            parameterValue.selectedValue = paramValue;
            parameterValue.persist();
        }
    }

    /**
     * Creates and persists ApiHeaderValue entities for a given map of headers.
     * This logic looks up the header definition by its name and associated system.
     *
     * @param headerValues The map of header names to values from the DTO.
     * @param arc The parent ApiRequestConfiguration that has already been persisted.
     * @param system The SourceSystem to which the header definitions are linked.
     */
    private void createAndPersistHeaderValues(Map<String, String> headerValues, ApiRequestConfiguration arc, SyncSystem system) {
        if (headerValues == null || headerValues.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : headerValues.entrySet()) {
            String headerName = entry.getKey();
            String headerValue = entry.getValue();


            ApiHeader headerDefinition = ApiHeader
                    .find("headerName = ?1 and syncSystem = ?2", headerName, system)
                    .firstResultOptional()
                    .map(ApiHeader.class::cast) // IDE type erasure
                    .orElseThrow(() -> new CoreManagementException("Header not defined",
                            "Header '" + headerName + "' is not defined for this system."));

            ApiHeaderValue headerValueEntity = new ApiHeaderValue();
            headerValueEntity.requestConfiguration = arc;
            headerValueEntity.apiHeader = headerDefinition;
            headerValueEntity.selectedValue = headerValue;

            headerValueEntity.persist();
        }
    }
}
