package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import jakarta.ws.rs.core.Response;

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

    /**
     * Update the stored OpenAPI specification for a given source system.
     *
     * @param sourceId the ID of the SourceSystem to update
     * @param spec     the OpenAPI spec content as a UTF-8 string
     * @throws CoreManagementWebException if the SourceSystem is not found
     */
    @Transactional
    public void updateOpenApiSpec(Long sourceId, String spec) {
        SourceSystem existing = SourceSystem.findById(sourceId);
        if (existing == null) {
            throw new CoreManagementWebException(
                Response.Status.NOT_FOUND,
                "Source system not found",
                "No source system found with id %d", sourceId);
        }
        existing.setOpenApi(spec);


    }
    @Transactional
    public void updateOpenApiSpecUrl(Long sourceId, String specUrl) {
        SourceSystem existing = SourceSystem.findById(sourceId);
        if (existing == null) {
            throw new CoreManagementWebException(
                Response.Status.NOT_FOUND,
                "Source system not found",
                "No source system found with id %d", sourceId);
        }
        existing.setOpenApiSpecUrl(specUrl);


    }
    }
