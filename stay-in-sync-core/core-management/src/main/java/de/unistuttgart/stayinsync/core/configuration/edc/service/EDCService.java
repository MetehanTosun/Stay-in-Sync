package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDC;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EDCService {

    public List<EDC> listAll() {
        return EDC.listAll();
    }

    public Optional<EDC> findById(Long id) {
        return Optional.ofNullable(EDC.findById(id));
    }

    public EDC create(EDC edc) {
        edc.persist();
        return edc;
    }

    public Optional<EDC> update(Long id, EDC updated) {
        return findById(id).map(existing -> {
            existing.name   = updated.name;
            existing.url    = updated.url;
            existing.apiKey = updated.apiKey;
            return existing;
        });
    }

    public boolean delete(Long id) {
        return EDC.deleteById(id);
    }
}
