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
import io.quarkus.hibernate.orm.panache.Panache;
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

        targetSystem.syncSystemEndpoints.add(entity);

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
        return TargetSystemEndpoint.list("targetSystem.id", targetSystemId);
    }

    @Transactional(SUPPORTS)
    public Optional<TargetSystemEndpoint> findTargetSystemEndpointById(Long id) {
        Log.debugf("Finding target-system-endpoint by id = %d", id);
        return TargetSystemEndpoint.findByIdOptional(id);
    }

    public void deleteTargetSystemEndpointById(Long id) {
        Log.infof("Deleting target-system-endpoint by id = %d", id);
        var em = Panache.getEntityManager();
        int rows = em.createQuery("delete from TargetSystemEndpoint t where t.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        Log.infof("Rows deleted: %d", rows);
        em.flush();
        em.clear();
    }

    public Optional<TargetSystemEndpoint> replaceTargetSystemEndpoint(@NotNull @Valid TargetSystemEndpoint endpoint) {
        Log.infof("Replacing target-system-endpoint id=%s method=%s path=%s", endpoint.id, endpoint.httpRequestType, endpoint.endpointPath);
        return TargetSystemEndpoint.findByIdOptional(endpoint.id)
                .map(TargetSystemEndpoint.class::cast)
                .map(existing -> {
                    Log.infof("Existing entity before update id=%s method=%s path=%s", existing.id, existing.httpRequestType, existing.endpointPath);
                    var em = Panache.getEntityManager();
                    var updated = em.createQuery(
                                    "update TargetSystemEndpoint t set t.endpointPath = :path, t.httpRequestType = :method, t.description = :desc, t.jsonSchema = :schema where t.id = :id")
                            .setParameter("path", endpoint.endpointPath)
                            .setParameter("method", endpoint.httpRequestType)
                            .setParameter("desc", endpoint.description)
                            .setParameter("schema", endpoint.jsonSchema)
                            .setParameter("id", endpoint.id)
                            .executeUpdate();
                    Log.infof("Rows updated: %d", updated);
                    em.flush();
                    em.clear();
                    var reloaded = TargetSystemEndpoint.findByIdOptional(endpoint.id).map(TargetSystemEndpoint.class::cast).orElse(null);
                    if (reloaded != null) {
                        Log.infof("Entity after update id=%s method=%s path=%s", reloaded.id, reloaded.httpRequestType, reloaded.endpointPath);
                    }
                    return reloaded;
                });
    }
}


