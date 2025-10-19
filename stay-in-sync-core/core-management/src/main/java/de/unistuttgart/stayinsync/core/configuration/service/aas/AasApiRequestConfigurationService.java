package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSourceApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.AasApiRequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.CreateAasArcDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class AasApiRequestConfigurationService {

    @Inject
    AasDtsGeneratorService aasDtsGeneratorService;

    @Inject
    AasApiRequestConfigurationMapper aasArcMapper;

    @Transactional
    public AasSourceApiRequestConfiguration create(CreateAasArcDTO dto) {
        SourceSystem ss = SourceSystem.findById(dto.sourceSystemId());
        if (ss == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "SourceSystem not found", "SourceSystem could not be found in the system");
        }

        AasSubmodelLite sm = AasSubmodelLite.findById(dto.submodelId());
        if (sm == null || !sm.sourceSystem.id.equals(ss.id)) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Submodel not found.", "Submodel not found or does not belong to the specified SourceSystem.");
        }

        AasSourceApiRequestConfiguration newAasArc = new AasSourceApiRequestConfiguration();
        newAasArc.sourceSystem = ss;
        newAasArc.submodel = sm;
        newAasArc.alias = dto.alias();
        newAasArc.active = dto.active();
        newAasArc.pollingIntervallTimeInMs = dto.pollingIntervallTimeInMs();

        String dts = aasDtsGeneratorService.generateDtsForSubmodel(sm);
        newAasArc.responseDts = dts;

        newAasArc.persist();
        return newAasArc;
    }

    public List<AasArcDTO> findBySourceSystemId(Long sourceSystemId) {
        List<AasSourceApiRequestConfiguration> arcs = AasSourceApiRequestConfiguration.list("sourceSystem.id", sourceSystemId);
        return arcs.stream().map(aasArcMapper::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public AasSourceApiRequestConfiguration update(Long arcId, CreateAasArcDTO dto) {
        AasSourceApiRequestConfiguration arcToUpdate = findById(arcId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "AAS Arc not found.", "AAS ARC with id " + arcId + " not found."));

        SourceSystem ss = SourceSystem.findById(dto.sourceSystemId());
        if (ss == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "SourceSystem not found.", "SourceSystem with id " + dto.sourceSystemId() + " not found.");
        }

        AasSubmodelLite sm = AasSubmodelLite.findById(dto.submodelId());
        if (sm == null || !sm.sourceSystem.id.equals(ss.id)) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Submodel not found.", "Submodel not found or does not belong to the specified SourceSystem.");
        }

        boolean submodelChanged = !arcToUpdate.submodel.id.equals(dto.submodelId());

        arcToUpdate.sourceSystem = ss;
        arcToUpdate.submodel = sm;
        arcToUpdate.alias = dto.alias();
        arcToUpdate.active = dto.active();
        arcToUpdate.pollingIntervallTimeInMs = dto.pollingIntervallTimeInMs();

        if (submodelChanged) {
            Log.infof("Submodel for AAS ARC %d changed. Regenerating DTS.", arcId);
            String dts = aasDtsGeneratorService.generateDtsForSubmodel(sm);
            arcToUpdate.responseDts = dts;
        }

        Log.infof("Updated AAS ARC with id %d.", arcId);
        return arcToUpdate;
    }

    @Transactional
    public void delete(Long arcId) {
        boolean deleted = AasSourceApiRequestConfiguration.deleteById(arcId);
        if (!deleted) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "AAS Arc not found.", "AAS ARC with id " + arcId + " not found.");
        }
        Log.infof("Deleted AAS ARC with id %d.", arcId);
    }

    public Optional<AasSourceApiRequestConfiguration> findById(Long arcId) {
        return AasSourceApiRequestConfiguration.findByIdOptional(arcId);
    }

}
