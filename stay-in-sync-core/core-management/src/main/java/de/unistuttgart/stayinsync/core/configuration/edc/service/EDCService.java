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
        // Persist setzt dank @PrePersist automatisch die UUID, wenn noch keine vergeben ist
        edc.persist();
        return edc;
    }

    public Optional<EDCInstance> update(UUID id, EDCInstance updated) {
        return findById(id).map(existing -> {
            existing.name   = updated.name;
            existing.url    = updated.url;
            existing.protocolVersion = updated.protocolVersion; // NEU
            existing.description     = updated.description;     // NEU
            existing.bpn             = updated.bpn;  
            if (updated.apiKey != null && !updated.apiKey.isBlank()) {
                existing.apiKey = updated.apiKey;
           }
            return existing;
        });
    }

    public boolean delete(UUID id) {
        return EDCInstance.deleteById(id);
    }
}
