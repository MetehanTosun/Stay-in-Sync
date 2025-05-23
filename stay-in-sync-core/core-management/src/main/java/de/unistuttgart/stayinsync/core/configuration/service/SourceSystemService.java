package de.unistuttgart.stayinsync.core.configuration.service;

import java.util.List;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.SourceSystem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SourceSystemService {

    public List<SourceSystem> findAllSourceSystems() {
        return SourceSystem.listAll(); // Panache
    }

    public SourceSystem findSourceSystemById(Long id) {
        return SourceSystem.findById(id); // Panache
    }

    @Transactional
    public void createSourceSystem(SourceSystem ss) {
        ss.persist(); // Panache
    }

    @Transactional
    public void updateSourceSystem(SourceSystem ss) {
        ss.persist(); // Panache kann update oder insert
    }

    @Transactional
    public void deleteSourceSystemById(Long id) {
        SourceSystem ss = SourceSystem.findById(id);
        if (ss != null)
            ss.delete();
    }
}
