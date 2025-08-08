package de.unistuttgart.stayinsync.core.configuration.service;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TargetSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateTargetSystemEndpointDTO;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Transactional(REQUIRED)
public class TargetSystemEndpointService {

    @Inject
    Validator validator;

    @Inject
    TargetSystemEndpointFullUpdateMapper mapper;

    @Inject
    TargetSystemService targetSystemService;

    public TargetSystemEndpoint persistTargetSystemEndpoint(@NotNull @Valid CreateTargetSystemEndpointDTO dto, Long targetSystemId) {
        Log.debugf("Persisting target-system-endpoint for target-system with id: %s", targetSystemId);

        TargetSystemEndpoint entity = mapper.mapToEntity(dto);

        TargetSystem targetSystem = targetSystemService.findById(targetSystemId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND,
                        "Unable to find Target System",
                        "There is no target-system with id %s", targetSystemId));

        entity.targetSystem = targetSystem;
        entity.syncSystem = targetSystem;
        entity.persist();
        return entity;
    }

    public List<TargetSystemEndpoint> persistTargetSystemEndpointList(@NotNull @Valid List<CreateTargetSystemEndpointDTO> endpoints, Long targetSystemId) {
        Log.debugf("Persisting target-system-endpoints for target-system with id: %s", targetSystemId);
        return endpoints.stream()
                .map(dto -> this.persistTargetSystemEndpoint(dto, targetSystemId))
                .collect(Collectors.toList());
    }

    @Transactional(SUPPORTS)
    public List<TargetSystemEndpoint> findAllEndpointsWithTargetSystemIdLike(Long targetSystemId) {
        Log.debugf("Finding all endpoints of target system with id = %s", targetSystemId);
        return Optional.ofNullable(TargetSystemEndpoint.findByTargetSystemId(targetSystemId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public Optional<TargetSystemEndpoint> findTargetSystemEndpointById(Long id) {
        Log.debugf("Finding target-system-endpoint by id = %d", id);
        return TargetSystemEndpoint.findByIdOptional(id);
    }

    public void deleteTargetSystemEndpointById(Long id) {
        Log.debugf("Deleting target-system-endpoint by id = %d", id);
        TargetSystemEndpoint.deleteById(id);
    }

    public Optional<TargetSystemEndpoint> replaceTargetSystemEndpoint(@NotNull @Valid TargetSystemEndpoint endpoint) {
        Log.debugf("Replacing target-system-endpoint: %s", endpoint);
        return TargetSystemEndpoint.findByIdOptional(endpoint.id)
                .map(TargetSystemEndpoint.class::cast)
                .map(target -> {
                    this.mapper.mapFullUpdate(endpoint, target);
                    return target;
                });
    }
}


