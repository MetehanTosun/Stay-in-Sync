package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationAssemblyDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.transport.dto.TransformationDeploymentFeedbackMessageDTO;
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

    public Transformation createTransformation(TransformationShellDTO dto) {
        Log.debugf("Creating new transformation shell with name: %s", dto.name());
        Transformation transformation = new Transformation();
        mapper.updateFromShellDTO(dto, transformation);
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

    public void updateDeploymentStatus(TransformationDeploymentFeedbackMessageDTO feedbackMessageDTO) {
        Optional<Transformation> apiRequestConfigurationById = findById(feedbackMessageDTO.transformationId());
        if(apiRequestConfigurationById.isEmpty()){
            Log.warnf("Unable to update deployment status of request configuration with id %d since no transformation was found using id", feedbackMessageDTO.transformationId());
        } else {
            Transformation sourceSystemApiRequestConfiguration = apiRequestConfigurationById.get();
            sourceSystemApiRequestConfiguration.deploymentStatus = feedbackMessageDTO.status();
            if(apiRequestConfigurationById.get().deploymentStatus.equals(JobDeploymentStatus.DEPLOYED))
                sourceSystemApiRequestConfiguration.workerHostName = feedbackMessageDTO.syncNode();
        }
    }
}
