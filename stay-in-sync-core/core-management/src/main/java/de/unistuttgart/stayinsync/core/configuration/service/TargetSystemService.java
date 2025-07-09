package de.unistuttgart.stayinsync.core.configuration.service;

import static jakarta.transaction.Transactional.TxType.*;

import java.util.List;
import java.util.Optional;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TargetSystemMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TargetSystemDTO;
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

    public TargetSystemDTO createTargetSystem(TargetSystemDTO dto) {
        TargetSystem entity = mapper.toEntity(dto);
        entity.persist();
        Log.infof("Created TargetSystem with id: %d", entity.id);
        return mapper.toDto(entity);
    }

    public TargetSystemDTO updateTargetSystem(Long id, TargetSystemDTO dto) {
        Log.infof("Attempting update of TargetSystem with id %d", id);

        TargetSystem entity = TargetSystem.<TargetSystem>findByIdOptional(id)
                .orElseThrow(() -> {
                    Log.warnf("Update failed: TargetSystem with id %d not found", id);
                    return new CoreManagementException(
                            Response.Status.NOT_FOUND,
                            "TargetSystem not found",
                            "TargetSystem with id %d not found.", id);
                });

        mapper.updateFromDto(dto, entity);
        return mapper.toDto(entity);
    }

    @Transactional(SUPPORTS)
    public Optional<TargetSystem> findById(Long id) {

        Log.debugf("Attempting to find TargetSystem with id %d", id);

        Optional<TargetSystem> result = TargetSystem.findByIdOptional(id);

        if (result.isEmpty()) {
            Log.infof("TargetSystem with id %d not found", id);
        } else {
            Log.debugf("Found TargetSystem with id %d", id);
        }
        return result;
    }

    @Transactional(SUPPORTS)
    public List<TargetSystemDTO> findAll() {
        Log.debug("Retrieving all TargetSystems from database.");

        List<TargetSystem> list = TargetSystem.listAll();
        Log.infof("Retrieved %d TargetSystems.", list.size());

        return list.stream()
                .map(mapper::toDto)
                .toList();
    }

    public boolean delete(Long id) {
        Log.debugf("Deleting TargetSystem with id %d", id);
        boolean deleted = TargetSystem.deleteById(id);

        if (deleted) {
            Log.infof("Successfully deleted TargetSystem with id %d", id);
        } else {
            Log.warnf("Failed to delete TargetSystem with id %d: not found", id);
        }

        return deleted;
    }
}
