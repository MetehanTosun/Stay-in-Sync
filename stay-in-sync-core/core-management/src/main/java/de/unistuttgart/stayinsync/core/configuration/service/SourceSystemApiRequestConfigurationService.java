package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemApiRequestConfigurationFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateRequestConfigurationDTO;
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
public class SourceSystemApiRequestConfigurationService {
    @Inject
    Validator validator;

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    SourceSystemApiRequestConfigurationFullUpdateMapper mapper;

    public SourceSystemApiRequestConfiguration persistApiRequestConfiguration(@NotNull @Valid CreateRequestConfigurationDTO sourceSystemApiRequestConfigurationDTO, Long endpointId) {
        SourceSystemApiRequestConfiguration sourceSystemApiRequestConfiguration = mapper.mapToEntity(sourceSystemApiRequestConfigurationDTO);

        Log.debugf("Persisting request-configurations: %s, for endpoint with id: %s", sourceSystemApiRequestConfiguration, endpointId);

        SourceSystemEndpoint sourceSystemEndpoint = sourceSystemEndpointService.findSourceSystemEndpointById(endpointId).orElseThrow(() -> {
            return new CoreManagementException("Unable to find Source System", "There is no source-system with id %s", endpointId);
        });
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
        return Optional.ofNullable(SourceSystemApiRequestConfiguration.findByendpointId(endpointId))
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

        Optional<SourceSystemApiRequestConfiguration> updatedApiRequestConfiguration = SourceSystemApiRequestConfiguration.findByIdOptional(sourceSystemApiRequestConfiguration.id)
                .map(SourceSystemApiRequestConfiguration.class::cast) // Only here for type erasure within the IDE
                .map(targetRequestConfiguration -> {
                    this.mapper.mapFullUpdate(sourceSystemApiRequestConfiguration, targetRequestConfiguration);
                    return targetRequestConfiguration;
                });

        return updatedApiRequestConfiguration;
    }
}
