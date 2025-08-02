package de.unistuttgart.stayinsync.core.configuration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemEndpointDTO;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class SourceSystemEndpointService {

    @Inject
    Validator validator;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    SourceSystemEndpointFullUpdateMapper sourceSystemEndpointFullMapper;

    public SourceSystemEndpoint persistSourceSystemEndpoint(@NotNull @Valid CreateSourceSystemEndpointDTO sourceSystemEndpointDTO, Long sourceSystemId) {
        System.out.println("### TEST persist: " + sourceSystemEndpointDTO.requestBodySchema());
        Log.debug("Persisting source-system-endpoint: " + sourceSystemEndpointDTO + ", for source-system with id: " + sourceSystemId);

        if (sourceSystemEndpointDTO.requestBodySchema() != null && !sourceSystemEndpointDTO.requestBodySchema().isBlank()) {
            try {
                objectMapper.readTree(sourceSystemEndpointDTO.requestBodySchema());
            } catch (JsonProcessingException e) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid JSON in requestBodySchema", "Das Feld requestBodySchema enthält kein valides JSON: %s", e.getMessage());
            }
        }

        if (sourceSystemEndpointDTO.responseBodySchema() != null && !sourceSystemEndpointDTO.responseBodySchema().isBlank()) {
            try {
                objectMapper.readTree(sourceSystemEndpointDTO.responseBodySchema());
            } catch (JsonProcessingException e) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid JSON in responseBodySchema", "Das Feld responseBodySchema enthält kein valides JSON: %s", e.getMessage());
            }
        }

        // Upsert-Logik: Prüfe, ob Endpoint existiert
        SourceSystemEndpoint existing = SourceSystemEndpoint.find("sourceSystem.id = ?1 and endpointPath = ?2 and httpRequestType = ?3",
                sourceSystemId,
                sourceSystemEndpointDTO.endpointPath(),
                sourceSystemEndpointDTO.httpRequestType()
        ).firstResult();

        SourceSystem sourceSystem = sourceSystemService.findSourceSystemById(sourceSystemId).orElseThrow(() ->
                new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find Source System", "There is no source-system with id " + sourceSystemId));

        if (existing != null) {
            // Update
            SourceSystemEndpoint updated = sourceSystemEndpointFullMapper.mapToEntity(sourceSystemEndpointDTO);
            existing.endpointPath = updated.endpointPath;
            existing.httpRequestType = updated.httpRequestType;
            existing.requestBodySchema = updated.requestBodySchema;
            existing.responseBodySchema = updated.responseBodySchema;
            existing.description = updated.description;
            // ggf. weitere Felder übernehmen
            existing.sourceSystem = sourceSystem;
            existing.syncSystem = sourceSystem;
            existing.persist();
            return existing;
        } else {
            // Neu anlegen
            SourceSystemEndpoint sourceSystemEndpoint = sourceSystemEndpointFullMapper.mapToEntity(sourceSystemEndpointDTO);
            sourceSystemEndpoint.requestBodySchema = sourceSystemEndpointDTO.requestBodySchema();
            sourceSystemEndpoint.responseBodySchema = sourceSystemEndpointDTO.responseBodySchema();
            sourceSystemEndpoint.sourceSystem = sourceSystem;
            sourceSystemEndpoint.syncSystem = sourceSystem;
            sourceSystemEndpoint.persist();
            return sourceSystemEndpoint;
        }
    }

    public List<SourceSystemEndpoint> persistSourceSystemEndpointList(@NotNull @Valid List<CreateSourceSystemEndpointDTO> endpoints, Long sourceSystemId) {
        Log.debug("Persisting source-system-endpoints for source-system with id: " + sourceSystemId);
        return endpoints.stream().map(endpointDTO -> this.persistSourceSystemEndpoint(endpointDTO, sourceSystemId)).collect(Collectors.toList());
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemEndpoint> findAllEndpointsWithSourceSystemIdLike(Long sourceSystemId) {
        Log.debugf("Finding all endpoints of source system with id = %s", sourceSystemId);
        return Optional.ofNullable(SourceSystemEndpoint.findBySourceSystemId(sourceSystemId))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public List<SourceSystemEndpoint> findAllSourceSystemEndpoints() {
        Log.debug("Getting all source-system-endpoints");
        return Optional.ofNullable(SourceSystemEndpoint.<SourceSystemEndpoint>listAll())
                .orElseGet(List::of);
    }


    @Transactional(SUPPORTS)
    public Optional<SourceSystemEndpoint> findSourceSystemEndpointById(Long id) {
        Log.debugf("Finding source-system-endpoint by id = %d", id);
        return SourceSystemEndpoint.findByIdOptional(id);
    }

    public void deleteSourceSystemEndpointById(Long id) {
        Log.debugf("Deleting endpoint by id = %d", id);
        SourceSystemEndpoint.deleteById(id);
    }

    public Optional<SourceSystemEndpoint> replaceSourceSystemEndpoint(@NotNull @Valid SourceSystemEndpoint sourceSystemEndpoint) {
        System.out.println("### TEST replace: " + sourceSystemEndpoint.requestBodySchema);
        Log.debug("Replacing endpoint: " + sourceSystemEndpoint);

      
        if (sourceSystemEndpoint.requestBodySchema != null && !sourceSystemEndpoint.requestBodySchema.isBlank()) {
            try {
                objectMapper.readTree(sourceSystemEndpoint.requestBodySchema);
            } catch (JsonProcessingException e) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid JSON in requestBodySchema", "Das Feld requestBodySchema enthält kein valides JSON: %s", e.getMessage());
            }
        }

        return SourceSystemEndpoint.findByIdOptional(sourceSystemEndpoint.id)
                .map(SourceSystemEndpoint.class::cast) // Only here for type erasure within the IDE
                .map(targetSouceSystemEndpoint -> {
                    this.sourceSystemEndpointFullMapper.mapFullUpdate(sourceSystemEndpoint, targetSouceSystemEndpoint);
                    // Explizit das Schema übernehmen
                    targetSouceSystemEndpoint.requestBodySchema = sourceSystemEndpoint.requestBodySchema;
                    System.out.println("### TEST replace (entity): " + targetSouceSystemEndpoint.requestBodySchema);
                    Log.info("Wird gespeichert (replace): " + targetSouceSystemEndpoint.requestBodySchema);
                    return targetSouceSystemEndpoint;
                });
    }

}
