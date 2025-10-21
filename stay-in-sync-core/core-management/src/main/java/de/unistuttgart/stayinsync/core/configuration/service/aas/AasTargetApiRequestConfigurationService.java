package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.AasTargetApiRequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasTargetArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.CreateAasTargetArcDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class AasTargetApiRequestConfigurationService {

    @Inject
    AasTargetApiRequestConfigurationMapper mapper;

    public AasTargetApiRequestConfiguration create(CreateAasTargetArcDTO dto) {
        Log.debugf("Attempting to create AAS Target ARC with alias '%s'", dto.alias());

        SourceSystem ts = SourceSystem.findById(dto.targetSystemId());
        if (ts == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "TargetSystem not found.", "TargetSystem was not found with id: " + dto.targetSystemId());
        }

        AasSubmodelLite sm = AasSubmodelLite.findById(dto.submodelId());
        // Very shallow comparison since AASSubmodelLite Entity doesn't support TargetSystem
        if (!Objects.equals(ts.apiType, "AAS")) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Submodel Mismatch", "Submodel not found or does not belong to the specified TargetSystem.");
        }

        AasTargetApiRequestConfiguration newArc = new AasTargetApiRequestConfiguration();
        newArc.targetSystem = ts;
        newArc.submodel = sm;
        newArc.alias = dto.alias();
        newArc.persist();

        Log.infof("Successfully created AAS Target ARC '%s' with ID %d", newArc.alias, newArc.id);
        return newArc;
    }

    public AasTargetApiRequestConfiguration update(Long id, CreateAasTargetArcDTO dto) {
        AasTargetApiRequestConfiguration arcToUpdate = findById(id)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "AAS Target ARC not found",  "AAS Target ARC with id " + id + " not found."));

        SourceSystem ts = SourceSystem.findById(dto.targetSystemId());
        AasSubmodelLite sm = AasSubmodelLite.findById(dto.submodelId());
        // Very shallow comparison since AASSubmodelLite Entity doesn't support TargetSystem
        if (ts == null || !Objects.equals(ts.apiType, "AAS")) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST,"Invalid Reference", "Invalid System or Submodel reference.");
        }

        arcToUpdate.targetSystem = ts;
        arcToUpdate.submodel = sm;
        arcToUpdate.alias = dto.alias();

        Log.infof("Updated AAS Target ARC with id %d.", id);
        return arcToUpdate;
    }

    public void delete(Long id) {
        boolean deleted = AasTargetApiRequestConfiguration.deleteById(id);
        if (!deleted) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "AAS Target ARC not found", "AAS Target ARC with id " + id + " not found.");
        }
        Log.infof("Deleted AAS Target ARC with id %d.", id);
    }

    public Optional<AasTargetApiRequestConfiguration> findById(Long id) {
        return AasTargetApiRequestConfiguration.findByIdOptional(id);
    }

    public List<AasTargetArcDTO> findAllByTransformationId(Long transformationId) {
        Transformation t = Transformation.findById(transformationId);
        if (t == null) return List.of();
        return t.aasTargetApiRequestConfigurations.stream()
                .map(mapper::mapToDto)
                .collect(Collectors.toList());
    }
}
