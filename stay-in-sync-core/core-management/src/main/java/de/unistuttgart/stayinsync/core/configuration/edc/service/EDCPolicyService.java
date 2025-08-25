package de.unistuttgart.stayinsync.core.configuration.edc.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;

@ApplicationScoped
public class EDCPolicyService {

    public List<EDCPolicy> listAll() {
        return EDCPolicy.listAll();
    }

    public Optional<EDCPolicy> findById(UUID id) {
        return EDCPolicy.findByIdOptional(id);
    }

    @Transactional
    public EDCPolicy create(EDCPolicy entity) {
        entity.persist();
        return entity;
    }

    @Transactional
    public Optional<EDCPolicy> update(UUID id, EDCPolicy newState) {
        EDCPolicy existing = EDCPolicy.findById(id);
        if (existing == null) {
            return Optional.empty();
        }
        existing.policyId = newState.policyId;
        existing.policyJson = newState.policyJson;
        return Optional.of(existing);
    }

    @Transactional
    public boolean delete(UUID id) {
        return EDCPolicy.deleteById(id);
    }
}
