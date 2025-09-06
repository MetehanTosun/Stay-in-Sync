package de.unistuttgart.stayinsync.core.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemApiRequestConfigurationFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.GetRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.util.TypeScriptTypeGenerator;
import de.unistuttgart.stayinsync.core.management.rabbitmq.producer.PollingJobMessageProducer;
import de.unistuttgart.stayinsync.transport.domain.ApiEndpointQueryParamType;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
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
import java.util.Set;

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
    SourceSystemApiRequestConfigurationFullUpdateMapper fullUpdateMapper;

    @Inject
    PollingJobMessageProducer pollingJobMessageProducer;

    // TODO: Evaluate if method is necessary since ARC creation has cascading effects
    public SourceSystemApiRequestConfiguration persistApiRequestConfiguration(@NotNull @Valid CreateRequestConfigurationDTO sourceSystemApiRequestConfigurationDTO, Long endpointId) {
        SourceSystemApiRequestConfiguration sourceSystemApiRequestConfiguration = fullUpdateMapper.mapToEntity(sourceSystemApiRequestConfigurationDTO);

        Log.debugf("Persisting request-configurations: %s, for endpoint with id: %s", sourceSystemApiRequestConfiguration, endpointId);

        SourceSystemEndpoint sourceSystemEndpoint = sourceSystemEndpointService.findSourceSystemEndpointById(endpointId).orElseThrow(() ->
                new CoreManagementException("Unable to find Source System", "There is no source-system with id %s", endpointId));

        sourceSystemApiRequestConfiguration.sourceSystem = sourceSystemEndpoint.sourceSystem;
        sourceSystemApiRequestConfiguration.sourceSystemEndpoint = sourceSystemEndpoint;
        sourceSystemEndpoint.apiRequestConfigurations.add(sourceSystemApiRequestConfiguration);
        sourceSystemApiRequestConfiguration.persist();


        return sourceSystemApiRequestConfiguration;
    }

    public void updateDeploymentStatus(Long requestConfigId, JobDeploymentStatus jobDeploymentStatus) {
        SourceSystemApiRequestConfiguration sourceSystemApiRequestConfiguration = findApiRequestConfigurationById(requestConfigId);

        if (isTransitioning(sourceSystemApiRequestConfiguration.deploymentStatus) && isTransitioning(jobDeploymentStatus)) {
            Log.warnf("The request config with id %d is currently in the deployment state of %s and thus can not be deployed or stopped", requestConfigId, sourceSystemApiRequestConfiguration.deploymentStatus);
        } else {
            Log.infof("Settings deployment status of request config with id %d to %s", requestConfigId, jobDeploymentStatus);
            sourceSystemApiRequestConfiguration.deploymentStatus = jobDeploymentStatus;
            switch (jobDeploymentStatus) {
                case DEPLOYING ->
                        pollingJobMessageProducer.publishPollingJob(fullUpdateMapper.mapToMessageDTO(sourceSystemApiRequestConfiguration));
                case STOPPING, RECONFIGURING ->
                        pollingJobMessageProducer.reconfigureDeployedPollingJob(fullUpdateMapper.mapToMessageDTO(sourceSystemApiRequestConfiguration));
            }
            ;
        }
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
    public SourceSystemApiRequestConfiguration findApiRequestConfigurationById(Long id) {
        Log.debugf("Finding request-configurations by id = %d", id);
        SourceSystemApiRequestConfiguration requestConfigById = SourceSystemApiRequestConfiguration.findById(id);
        if (requestConfigById == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find api request configuration", "No api request config with id %d exists", id);
        }
        return requestConfigById;
    }

    public void deleteApiRequestConfigurationById(Long id) {
        Log.debugf("Deleting request-configurations by id = %d", id);
        SourceSystemApiRequestConfiguration.deleteById(id);
    }

    /**
     * Replaces an existing ARC with the data provided in the DTO.
     * This is a full update: old parameters and headers are cleared and replaced.
     *
     * @param id The ID of the ARC to update.
     * @param dto The DTO containing the new configuration.
     * @return An Optional containing the updated entity, or empty if not found.
     */
    @Transactional
    public Optional<GetRequestConfigurationDTO> update(Long id, CreateSourceArcDTO dto) {
        Log.debugf("Attempting to update ARC with id %d", id);

        return SourceSystemApiRequestConfiguration.<SourceSystemApiRequestConfiguration>findByIdOptional(id)
                .map(arcToUpdate -> {
                    arcToUpdate.queryParameterValues.clear();
                    arcToUpdate.apiRequestHeaders.clear();
                    ApiEndpointQueryParamValue.delete("requestConfiguration.id", id);
                    ApiHeaderValue.delete("requestConfiguration.id", id);

                    arcToUpdate.alias = dto.alias();
                    arcToUpdate.active = dto.active();
                    arcToUpdate.pollingIntervallTimeInMs = dto.pollingIntervallTimeInMs();

                    try {
                        JsonNode rootNode = new ObjectMapper().readTree(dto.responseDts());
                        arcToUpdate.responseIsArray = rootNode.isArray();
                        arcToUpdate.responseDts = typeGenerator.generate(dto.responseDts());
                    } catch (JsonProcessingException e) {
                        throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid sample JSON", e.getMessage());
                    }

                    createAndPersistParameterValues(dto.pathParameterValues(), arcToUpdate, arcToUpdate.sourceSystemEndpoint, ApiEndpointQueryParamType.PATH);
                    createAndPersistParameterValues(dto.queryParameterValues(), arcToUpdate, arcToUpdate.sourceSystemEndpoint, ApiEndpointQueryParamType.QUERY);
                    createAndPersistHeaderValues(dto.headerValues(), arcToUpdate, arcToUpdate.sourceSystem);

                    Log.infof("Successfully updated ARC with id %d", id);
                    return fullUpdateMapper.mapToDTOGet(arcToUpdate);
                });
    }

    @Transactional
    public SourceSystemApiRequestConfiguration create(CreateSourceArcDTO dto, Long endpointId) {
        SourceSystem sourceSystem = SourceSystem.<SourceSystem>findByIdOptional(dto.sourceSystemId())
                .orElseThrow(() -> new NotFoundException("SourceSystem not found."));
        SourceSystemEndpoint endpoint = SourceSystemEndpoint.<SourceSystemEndpoint>findByIdOptional(endpointId)
                .orElseThrow(() -> new NotFoundException("SourceSystemEndpoint not found."));

        if (!endpoint.sourceSystem.id.equals(sourceSystem.id)) {
            throw new CoreManagementException("Source System mismatch", "Endpoint does not belong to the specified source system.");
        }

        String jsonSample = dto.responseDts();
        boolean isArray = false;
        String generatedInterfaces = "";
        try {
            JsonNode rootNode = new ObjectMapper().readTree(jsonSample);
            if (rootNode.isArray()) {
                isArray = true;
            }
            generatedInterfaces = typeGenerator.generate(jsonSample);
        } catch (JsonProcessingException e) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid sample JSON", "The provided JSON is invalid and cannot be built into a TypeScript type: %s", e.getMessage());
        }

        SourceSystemApiRequestConfiguration newArc = fullUpdateMapper.mapToEntity(dto);

        newArc.sourceSystem = sourceSystem;
        newArc.sourceSystemEndpoint = endpoint;
        newArc.syncSystemEndpoint = endpoint;
        newArc.responseDts = generatedInterfaces;
        newArc.responseIsArray = isArray;

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

            ApiEndpointQueryParam paramDefinition = ApiEndpointQueryParam
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
     * @param arc          The parent ApiRequestConfiguration that has already been persisted.
     * @param system       The SourceSystem to which the header definitions are linked.
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

    private boolean isTransitioning(JobDeploymentStatus jobDeploymentStatus) {
        return Set.of(JobDeploymentStatus.DEPLOYING, JobDeploymentStatus.RECONFIGURING, JobDeploymentStatus.STOPPING).contains(jobDeploymentStatus);
    }

    public void undeployAllUnused() {
        List<SourceSystemApiRequestConfiguration> sourceSystemApiRequestConfigurations = SourceSystemApiRequestConfiguration.listAllActiveAndUnused();
        Log.infof("%d unused polling configurations have been found and will be scheduled for undeployment", sourceSystemApiRequestConfigurations.size());
        sourceSystemApiRequestConfigurations.stream().forEach(apiRequestConfiguration -> updateDeploymentStatus(apiRequestConfiguration.id, JobDeploymentStatus.STOPPING));
    }

}
