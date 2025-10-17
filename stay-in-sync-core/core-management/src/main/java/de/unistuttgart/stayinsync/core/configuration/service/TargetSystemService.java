package de.unistuttgart.stayinsync.core.configuration.service;

import static jakarta.transaction.Transactional.TxType.*;

import java.util.List;
import java.util.Optional;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiQueryParam;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestHeader;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemVariable;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiEndpointQueryParamValue;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.ApiHeaderValue;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfigurationAction;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig.SyncSystemAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.TargetSystemMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Transactional(REQUIRED)
public class TargetSystemService {

    @Inject
    TargetSystemMapper mapper;

    @Inject
    OpenApiSpecificationParserService openApiSpecificationParserService;

    public TargetSystemDTO createTargetSystem(TargetSystemDTO dto) {
        Log.debugf("Creating new TargetSystem with id: %d", dto.id());
        TargetSystem entity = mapper.toEntity(dto);
        entity.persist();
        return mapper.toDto(entity);
    }

    public TargetSystemDTO updateTargetSystem(Long id, TargetSystemDTO dto) {
        Log.debugf("Updating TargetSystem with id %d", id);

        TargetSystem entity = TargetSystem.<TargetSystem>findByIdOptional(id)
                .orElseThrow(() -> new CoreManagementException(
                        Response.Status.NOT_FOUND,
                        "TargetSystem not found",
                        "TargetSystem with id %d not found.", id));

        mapper.updateFromDto(dto, entity);
        return mapper.toDto(entity);
    }

    @Transactional(SUPPORTS)
    public Optional<TargetSystem> findById(Long id) {
        Log.debugf("Finding TargetSystem with id %d", id);
        return TargetSystem.findByIdOptional(id);
    }

    @Transactional(SUPPORTS)
    public List<TargetSystemDTO> findAll() {
        Log.debug("Getting all TargetSystems.");
        return TargetSystem.<TargetSystem>listAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public boolean delete(Long id) {
        Log.debugf("Deleting TargetSystem with id %d", id);
        Optional<TargetSystem> targetOpt = TargetSystem.findByIdOptional(id);
        if (targetOpt.isEmpty()) {
            Log.warnf("No target system found with id %d", id);
            return false;
        }
        TargetSystem target = targetOpt.get();

        // Delete endpoints and their children (instance-based, like SourceSystem)
        List<TargetSystemEndpoint> endpoints = TargetSystemEndpoint.findByTargetSystemId(id);
        for (TargetSystemEndpoint endpoint : endpoints) {
            // Variables
            List<TargetSystemVariable> variables = TargetSystemVariable.list("targetSystemEndpoint.id", endpoint.id);
            for (TargetSystemVariable v : variables) v.delete();

            // Param values linked via queryParam.syncSystemEndpoint
            List<ApiEndpointQueryParamValue> qpv = ApiEndpointQueryParamValue.list("queryParam.syncSystemEndpoint.id", endpoint.id);
            for (ApiEndpointQueryParamValue val : qpv) val.delete();

            // Generic query params
            List<ApiEndpointQueryParam> qps = ApiEndpointQueryParam.findByEndpointId(endpoint.id);
            for (ApiEndpointQueryParam qp : qps) qp.delete();

            // Target-specific query params and endpoint headers
            List<TargetSystemApiQueryParam> tParams = TargetSystemApiQueryParam.list("targetSystemEndpoint.id", endpoint.id);
            for (TargetSystemApiQueryParam tp : tParams) tp.delete();

            List<TargetSystemApiRequestHeader> tHeaders = TargetSystemApiRequestHeader.list("targetSystemEndpoint.id", endpoint.id);
            for (TargetSystemApiRequestHeader th : tHeaders) th.delete();

            // EDC Asset
            List<EDCAsset> assets = EDCAsset.list("targetSystemEndpoint.id", endpoint.id);
            for (EDCAsset asset : assets) asset.delete();

            // Endpoint
            endpoint.delete();
        }

        List<TargetSystemApiRequestConfiguration> tConfigs = TargetSystemApiRequestConfiguration.list("targetSystem.id", id);
        for (TargetSystemApiRequestConfiguration cfg : tConfigs) {
            List<TargetSystemApiRequestConfigurationAction> actions = TargetSystemApiRequestConfigurationAction.list("targetSystemApiRequestConfiguration.id", cfg.id);
            for (TargetSystemApiRequestConfigurationAction action : actions) action.delete();
            cfg.delete();
        }

        // API headers and their values
        List<ApiHeader> headers = ApiHeader.list("syncSystem.id", id);
        for (ApiHeader h : headers) {
            List<ApiHeaderValue> vals = ApiHeaderValue.list("apiHeader.id", h.id);
            for (ApiHeaderValue hv : vals) hv.delete();
            h.delete();
        }

        // Auth config
        List<SyncSystemAuthConfig> auths = SyncSystemAuthConfig.list("syncSystem.id", id);
        for (SyncSystemAuthConfig ac : auths) ac.delete();

        // Finally, delete TargetSystem
        target.delete();
        return true;
    }
}