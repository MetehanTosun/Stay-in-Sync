package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCInstance;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EDCService {

    public List<EDCInstance> listAll() {
        return EDCInstance.listAll();
    }

    public Optional<EDCInstance> findById(UUID id) {
        return Optional.ofNullable(EDCInstance.findById(id));
    }

    public EDCInstance create(EDCInstance edc) {
        edc.persist();
        return edc;
    }

    public Optional<EDCInstance> update(UUID id, EDCInstance updated) {
        return findById(id).map(existing -> {
            existing.name            = updated.name;
            existing.url             = updated.url;
            existing.apiKey          = updated.apiKey;
            existing.protocolVersion = updated.protocolVersion;
            existing.description     = updated.description;
            existing.bpn             = updated.bpn;
            return existing;
        });
    }

    public boolean delete(UUID id) {
        return EDCInstance.deleteById(id);
    }
}
