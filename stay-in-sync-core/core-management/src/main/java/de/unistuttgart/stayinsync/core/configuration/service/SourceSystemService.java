package de.unistuttgart.stayinsync.core.configuration.service;

import java.util.List;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.SourceSystem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class SourceSystemService {

    public List<SourceSystem> findAllSourceSystems() {
        return SourceSystem.listAll(); // Panache
    }

    public SourceSystem findSourceSystemById(Long id) {
        SourceSystem existingSs = SourceSystem.findById(id);
        if (existingSs == null) {
            throw new CoreManagementWebException(Response.Status.NOT_FOUND, "Source system not found",
                    "No source system found with id %d", id);
        }

        return SourceSystem.findById(id); // Panache
    }

    @Transactional
    public void createSourceSystem(SourceSystem ss) {
        /*
         * TODO: Validation logic, as soon as we know how the final Model of a
         * SourceSystem looks like.
         */
        ss.persist(); // Panache
    }

    @Transactional
    public void updateSourceSystem(SourceSystem ss) {
        SourceSystem existingSs = SourceSystem.findById(ss);
        if (existingSs == null) {
            throw new CoreManagementWebException(Response.Status.NOT_FOUND, "Source system not found",
                    "No source system found with id %d", ss.id);
        }
        ss.persist(); // Panache kann update oder insert
    }

    @Transactional
    public void deleteSourceSystemById(Long id) {
        SourceSystem existingSs = SourceSystem.findById(id);
        if (existingSs == null) {
            throw new CoreManagementWebException(
                    Response.Status.NOT_FOUND,
                    "Source system not found",
                    "No source system found with id %d", id);
        }
        existingSs.delete();
    }
}