package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemEndpointDTO;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class SourceSystemEndpointService {

    @Inject
    Validator validator;

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    SourceSystemEndpointFullUpdateMapper sourceSystemEndpointFullMapper;

    public SourceSystemEndpoint persistSourceSystemEndpoint(@NotNull @Valid CreateSourceSystemEndpointDTO sourceSystemEndpointDTO, Long sourceSystemId) {
        Log.debugf("Persisting source-system-endpoint: %s, for source-system with id: %s", sourceSystemEndpointDTO, sourceSystemId);

        SourceSystemEndpoint sourceSystemEndpoint = sourceSystemEndpointFullMapper.mapToEntity(sourceSystemEndpointDTO);

        SourceSystem sourceSystem = sourceSystemService.findSourceSystemById(sourceSystemId).orElseThrow(() -> {
            return new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find Source System", "There is no source-system with id %s", sourceSystemId);
        });
        sourceSystemEndpoint.sourceSystem = sourceSystem;
        sourceSystemEndpoint.persist();

        return sourceSystemEndpoint;
    }

    public List<SourceSystemEndpoint> persistSourceSystemEndpointList(@NotNull @Valid List<CreateSourceSystemEndpointDTO> endpoints, Long sourceSystemId) {
        Log.debugf("Persisting source-system-endpoints: %s, for source-system with id: %s", sourceSystemId);
        return endpoints.stream().map(endpointDTO -> this.persistSourceSystemEndpoint(endpointDTO, sourceSystemId)).collect(Collectors.toList());
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemEndpoint> findAllEndpointsWithSourceSystemIdLike(Long sourceSystemId) {
        Log.debugf("Finding all endpoints of source system with id = %s", sourceSystemId);
        return Optional.ofNullable(SourceSystemEndpoint.findBySourceSystemId(sourceSystemId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemEndpoint> findAllSourceSystemEndpoints() {
        Log.debug("Getting all source-system-endpoints");
        return Optional.ofNullable(SourceSystemEndpoint.<SourceSystemEndpoint>listAll())
                .orElseGet(List::of);
    }


    @Transactional(SUPPORTS)
    public Optional<SourceSystemEndpoint> findSourceSystemEndpointById(Long id) {
        Log.debugf("Finding source-system-endpoint by id = %d", id);
        return SourceSystemEndpoint.findByIdOptional(id);
    }

    public void deleteSourceSystemEndpointById(Long id) {
        Log.debugf("Deleting endpoint by id = %d", id);
        SourceSystemEndpoint.deleteById(id);
    }

    public Optional<SourceSystemEndpoint> replaceSourceSystemEndpoint(@NotNull @Valid SourceSystemEndpoint sourceSystemEndpoint) {
        Log.debugf("Replacing endpoint: %s", sourceSystemEndpoint);

        Optional<SourceSystemEndpoint> updatedSourceSystemEndpoint = SourceSystemEndpoint.findByIdOptional(sourceSystemEndpoint.id)
                .map(SourceSystemEndpoint.class::cast) // Only here for type erasure within the IDE
                .map(targetSouceSystemEndpoint -> {
                    this.sourceSystemEndpointFullMapper.mapFullUpdate(sourceSystemEndpoint, targetSouceSystemEndpoint);
                    return targetSouceSystemEndpoint;
                });

        return updatedSourceSystemEndpoint;
    }

}
