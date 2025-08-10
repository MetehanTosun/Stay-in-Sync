package de.unistuttgart.stayinsync.core.configuration.service;

import static jakarta.transaction.Transactional.TxType.*;

import java.util.List;
import java.util.Optional;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
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
        return TargetSystem.deleteById(id);
    }
}
