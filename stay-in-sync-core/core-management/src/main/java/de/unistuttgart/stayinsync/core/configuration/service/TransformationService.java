package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.RequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationAssemblyDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.GetRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.UpdateTransformationRequestConfigurationDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class TransformationService {

    @Inject
    TransformationMapper mapper;

    @Inject
    RequestConfigurationMapper requestConfigurationMapper;

    public Transformation createTransformation(TransformationShellDTO dto) {
        Log.debugf("Creating new transformation shell with name: %s", dto.name());
        Transformation transformation = new Transformation();
        mapper.updateFromShellDTO(dto, transformation);

        TransformationScript script = new TransformationScript();
        script.name = dto.name() + " Script";
        script.typescriptCode = "";

        script.transformation = transformation;
        transformation.transformationScript = script;

        transformation.persist();
        return transformation;
    }

    public Transformation updateTransformation(Long transformationId, TransformationAssemblyDTO dto) {
        Log.debugf("Assembling transformation with id %d", transformationId);

        Transformation transformation = Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Transformation not found", "Transformation with id %d not found.", transformationId));


        TransformationScript script = null;
        if (dto.transformationScriptId() != null) {
            script = TransformationScript.<TransformationScript>findByIdOptional(dto.transformationScriptId())
                    .orElseThrow(() -> new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid Script ID", "TransformationScript with id %d not found.", dto.transformationScriptId()));
        }

        TransformationRule rule = TransformationRule.<TransformationRule>findByIdOptional(dto.transformationRuleId())
                .orElseThrow(() -> new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid Rule ID", "TransformationRule with id %d not found.", dto.transformationRuleId()));

        Set<SourceSystemEndpoint> sourceEndpoints = dto.sourceSystemEndpointIds().stream()
                .map(id -> SourceSystemEndpoint.<SourceSystemEndpoint>findByIdOptional(id)
                        .orElseThrow(() -> new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid SourceSystemEndpoint ID", "SourceSystemEndpoint with id %d not found.", id)))
                .collect(Collectors.toSet());

        transformation.transformationScript = script;
        transformation.transformationRule = rule;
        //TODO: replace with api request configs
        //transformation.sourceSystemEndpoints = sourceEndpoints;

        if (script != null) {
            script.transformation = transformation;
        }
        
        if (rule != null) {
            rule.transformation = transformation;
        }

        return transformation;
    }

    public List<GetRequestConfigurationDTO> getTargetArcs(Long transformationId){
        Log.debugf("Getting Target ARCs for Transformation with id %d", transformationId);

        Transformation transformation = Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Transformation not found", "Transformation with id %d not found.", transformationId));

        Set<TargetSystemApiRequestConfiguration> targetArcs = transformation.targetSystemApiRequestConfigurations;
        List<TargetSystemApiRequestConfiguration> targetArcsToList =  targetArcs.stream().toList();
        return requestConfigurationMapper.mapToGetDTOList(targetArcsToList);
    }

    @Transactional
    public Transformation updateTargetArcs(Long transformationId, UpdateTransformationRequestConfigurationDTO dto){
        Log.debugf("Updating Target ARCs for Transformation with id %d", transformationId);

        Transformation transformation = Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Transformation not found", "Transformation with id %d not found.", transformationId));

        transformation.targetSystemApiRequestConfigurations.clear();

        if (dto.targetArcIds() != null && !dto.targetArcIds().isEmpty()) {
            List<TargetSystemApiRequestConfiguration> arcsToLink = TargetSystemApiRequestConfiguration.list("id in ?1", dto.targetArcIds());

            if (arcsToLink.size() != dto.targetArcIds().size()) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid ARC ID", "One or more provided Target ARC IDs could not be found.");
            }

            transformation.targetSystemApiRequestConfigurations.addAll(arcsToLink);
        }

        Log.infof("Successfully updated Target ARCs for Transformation %d. New count: %d",
                transformationId, transformation.targetSystemApiRequestConfigurations.size());

        return transformation;
    }

    @Transactional(SUPPORTS)
    public Optional<Transformation> findById(Long id) {
        Log.debugf("Finding transformation with id %d", id);
        return Transformation.findByIdOptional(id);
    }

    @Transactional(SUPPORTS)
    public Transformation findByIdDirect(Long id) {
        Log.debugf("Finding transformation with id %d", id);
        return Transformation.findById(id);
    }

    public Optional<TransformationScript> findScriptById(Long transformationId) {
        Log.debugf("Finding script with Transformation id %d", transformationId);
        Optional<Transformation> transformation = Transformation.findByIdOptional(transformationId);

        if (transformation.isPresent()) {
            return Optional.ofNullable(transformation.get().transformationScript);
        } else {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find Transformation", "There is no transformation with id %s", transformationId);
        }
    }

    @Transactional(SUPPORTS)
    public List<Transformation> findAll() {
        Log.debug("Getting all transformations.");
        return Transformation.listAll();
    }

    public boolean delete(Long id) {
        Log.debugf("Deleting transformation with id %d", id);
        return Transformation.deleteById(id);
    }
}
