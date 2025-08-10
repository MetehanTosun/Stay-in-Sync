package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EDCPropertyService {

    public List<EDCProperty> listAll() {
        return EDCProperty.listAll();
    }

    public Optional<EDCProperty> findById(UUID id) {
        return EDCProperty.findByIdOptional(id);
    }

    @Transactional
    public EDCProperty create(EDCProperty entity) {
        entity.persist();
        return entity;
    }

    @Transactional
    public Optional<EDCProperty> update(UUID id, EDCProperty newState) {
        return findById(id)
            .map(existing -> {
                existing.description = newState.description;
                return existing;
            });
    }

    @Transactional
    public boolean delete(UUID id) {
        return EDCProperty.deleteById(id);
    }
}
