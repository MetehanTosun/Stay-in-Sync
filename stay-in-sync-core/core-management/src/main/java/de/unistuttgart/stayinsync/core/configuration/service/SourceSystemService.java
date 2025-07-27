package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SourceSystemService {
    @Inject
    SourceSystemFullUpdateMapper mapper;

    @Inject
    OpenApiSpecificationParserService openApiSpecificationParserService;

    public List<SourceSystem> findAllSourceSystems() {
        Log.debug("Fetching all source systems");
        return SourceSystem.listAll(); // Panache
    }

    public Optional<SourceSystem> findSourceSystemById(Long id) {
        Log.debugf("Fetching source system with ID: %d", id);
        return SourceSystem.findByIdOptional(id);
    }

    @Transactional
    public SourceSystem createSourceSystem(CreateSourceSystemDTO sourceSystemDTO) {
        /*
         * TODO: Validation logic, as soon as we know how the final Model of a
         * SourceSystem looks like.
         */
        Log.debugf("Creating new source system with name: %s", sourceSystemDTO.name());
        SourceSystem sourceSystem = mapper.mapToEntity(sourceSystemDTO);

        if (sourceSystemDTO.openApiSpec() != null && !sourceSystemDTO.openApiSpec().isBlank()) {
            sourceSystem.openApiSpec = sourceSystemDTO.openApiSpec();
        }

        sourceSystem.persist();
        openApiSpecificationParserService.synchronizeFromSpec(sourceSystem);
        return sourceSystem;
    }

    @Transactional
    public Optional<SourceSystem> updateSourceSystem(CreateSourceSystemDTO sourceSystemDTO) {
        Log.debugf("Updating source system with ID: %d", sourceSystemDTO.id());
        SourceSystem existingSs = SourceSystem.findById(sourceSystemDTO.id());
        if (existingSs != null) {
            mapper.mapFullUpdate(mapper.mapToEntity(sourceSystemDTO), existingSs);
            openApiSpecificationParserService.synchronizeFromSpec(existingSs);
        }
        return Optional.ofNullable(existingSs);
    }

    @Transactional
    public boolean deleteSourceSystemById(Long id) {
        Log.debugf("Deleting source system with ID: %d", id);
        return SourceSystem.deleteById(id);
    }
}