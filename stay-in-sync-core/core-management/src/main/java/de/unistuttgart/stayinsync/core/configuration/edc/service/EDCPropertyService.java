package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EDCPropertyService {

    public List<EDCProperty> listAll() {
        return EDCProperty.listAll();
    }

    public Optional<EDCProperty> findById(Long id) {
        return Optional.ofNullable(EDCProperty.findById(id));
    }

    @Transactional
    public EDCProperty create(EDCProperty entity) {
        entity.persist();
        return entity;
    }

    @Transactional
    public Optional<EDCProperty> update(Long id, EDCProperty newState) {
        return findById(id)
            .map(existing -> {
                existing.description = newState.description;
                return existing;
            });
    }

    @Transactional
    public boolean delete(Long id) {
        return EDCProperty.deleteById(id);
    }
}
