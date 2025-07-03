package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import jakarta.ws.rs.core.Response;

/**
 * Service for managing SourceSystem entities, including CRUD operations
 * and OpenAPI specification updates.
 */
@ApplicationScoped
public class SourceSystemService {
    @Inject
    SourceSystemFullUpdateMapper mapper;

    /**
     * Retrieve all source systems from the database.
     *
     * @return list of all SourceSystem entities
     */
    public List<SourceSystem> findAllSourceSystems() {
        Log.debug("Fetching all source systems");
        return SourceSystem.listAll(); // Panache
    }

    /**
     * Find a source system by its ID.
     *
     * @param id the unique identifier of the source system
     * @return Optional containing the SourceSystem if found, or empty otherwise
     */
    public Optional<SourceSystem> findSourceSystemById(Long id) {
        Log.debugf("Fetching source system with ID: %d", id);
        return SourceSystem.findByIdOptional(id);
    }

    /**
     * Persist a new source system to the database.
     *
     * @param ss the SourceSystem entity to create
     */
    @Transactional
    public SourceSystem createSourceSystem(CreateSourceSystemDTO sourceSystemDTO) {
        /*
         * TODO: Validation logic, as soon as we know how the final Model of a
         * SourceSystem looks like.
         */
        Log.debugf("Creating new source system with name: %s", sourceSystemDTO.name());
        SourceSystem sourceSystem = mapper.mapToEntity(sourceSystemDTO);
        sourceSystem.persist();
        return sourceSystem;
    }

    /**
     * Update an existing source system with new values.
     *
     * @param ss the SourceSystem entity containing updated fields (must include id)
     * @return Optional containing the updated entity, or empty if not found
     */
    @Transactional
    public Optional<SourceSystem> updateSourceSystem(CreateSourceSystemDTO sourceSystemDTO) {
        Log.debugf("Updating source system with ID: %d", sourceSystemDTO.id());
        SourceSystem existingSs = SourceSystem.findById(sourceSystemDTO.id());
        if (existingSs != null) {
            mapper.mapFullUpdate(mapper.mapToEntity(sourceSystemDTO), existingSs);
        }
        return Optional.ofNullable(existingSs);
    }

    /**
     * Delete a source system by its ID.
     *
     * @param id the ID of the source system to delete
     * @return true if deletion was successful, false otherwise
     */
    @Transactional
    public boolean deleteSourceSystemById(Long id) {
        Log.debugf("Deleting source system with ID: %d", id);
        boolean deleted = SourceSystem.deleteById(id);
        return deleted;
    }

    /**
     * Update the stored raw OpenAPI specification for a source system.
     *
     * @param sourceId the ID of the source system
     * @param spec the OpenAPI specification content (JSON/YAML)
     * @throws CoreManagementWebException if the source system does not exist
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
   


    /**
     * Update the OpenAPI specification URL and fetch the spec content.
     *
     * @param sourceId the ID of the source system
     * @param specUrl URL pointing to the OpenAPI spec
     * @throws CoreManagementWebException if the source system does not exist or fetch fails
     */
    @Transactional
    public void updateOpenApiSpecUrl(Long sourceId, String specUrl) {
        SourceSystem existing = SourceSystem.findById(sourceId);
        if (existing == null) {
            throw new CoreManagementWebException(
                Response.Status.NOT_FOUND,
                "Source system not found",
                "No source system found with id %d", sourceId);
        }

        // 1) speichere zuerst die URL
        existing.setOpenApiSpecUrl(specUrl);

        // 2) jetzt die Spec per HTTP holen
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(specUrl))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request,
                BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                existing.setOpenApi(response.body());
            } else {
                throw new CoreManagementWebException(
                    Response.Status.BAD_GATEWAY,
                    "Failed to fetch OpenAPI spec",
                    "Remote server returned HTTP %d", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new CoreManagementWebException(
                Response.Status.BAD_GATEWAY,
                "Failed to fetch OpenAPI spec",
                e.getMessage(),
                e);
        }
    }
}
