package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAccessPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EDCAccessPolicyService {

    public List<EDCAccessPolicy> listAll() {
        return EDCAccessPolicy.listAll();
    }

    public Optional<EDCAccessPolicy> findById(UUID id) {
        return EDCAccessPolicy.findByIdOptional(id);
    }

    @Transactional
    public EDCAccessPolicy create(EDCAccessPolicy entity) {
        entity.persist();
        return entity;
    }

    @Transactional
    public Optional<EDCAccessPolicy> update(UUID id, EDCAccessPolicy newState) {
        EDCAccessPolicy existing = EDCAccessPolicy.findById(id);
        if (existing == null) {
            return Optional.empty();
        }
        existing.setEdcAsset(newState.getEdcAsset());
        existing.setAccessPolicyPermissions(newState.getAccessPolicyPermissions());
        return Optional.of(existing);
    }

    @Transactional
    public boolean delete(UUID id) {
        return EDCAccessPolicy.deleteById(id);
    }
}
