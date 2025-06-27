package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SourceSystemService {
    @Inject
    SourceSystemFullUpdateMapper mapper;

    public List<SourceSystem> findAllSourceSystems() {
        Log.debug("Fetching all source systems");
        return SourceSystem.listAll(); // Panache
    }

    public Optional<SourceSystem> findSourceSystemById(Long id) {
        Log.debugf("Fetching source system with ID: %d", id);
        return SourceSystem.findByIdOptional(id);
    }

    @Transactional
    public void createSourceSystem(SourceSystem ss) {
        /*
         * TODO: Validation logic, as soon as we know how the final Model of a
         * SourceSystem looks like.
         */
        Log.debugf("Creating new source system with name: %s", ss.name);
        ss.persist(); // Panache
    }

    @Transactional
    public Optional<SourceSystem> updateSourceSystem(SourceSystem ss) {
        Log.debugf("Updating source system with ID: %d", ss.id);
        SourceSystem existingSs = SourceSystem.findById(ss.id);
        if (existingSs != null) {
            mapper.mapFullUpdate(ss, existingSs);
        }
        return Optional.ofNullable(existingSs);
    }

    @Transactional
    public boolean deleteSourceSystemById(Long id) {
        Log.debugf("Deleting source system with ID: %d", id);
        boolean deleted = SourceSystem.deleteById(id);
        return deleted;
    }
}
