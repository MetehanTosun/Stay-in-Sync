package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAccessPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EDCAccessPolicyService {

    public List<EDCAccessPolicy> listAll() {
        return EDCAccessPolicy.listAll();
    }

    public Optional<EDCAccessPolicy> findById(Long id) {
        return EDCAccessPolicy.findByIdOptional(id);
    }

    @Transactional
    public EDCAccessPolicy createFromDto(EDCAccessPolicy entity) {
        entity.persist();
        return entity;
    }

    @Transactional
    public EDCAccessPolicy update(Long id, EDCAccessPolicy newState) {
        EDCAccessPolicy existing = EDCAccessPolicy.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("AccessPolicy " + id + " nicht gefunden");
        }
        // Asset und Permissions übernehmen
        existing.setEdcAsset(newState.getEdcAsset());
        existing.setAccessPolicyPermissions(newState.getAccessPolicyPermissions());
        return existing;
    }

    @Transactional
    public boolean delete(Long id) {
        return EDCAccessPolicy.deleteById(id);
    }
}
