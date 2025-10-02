package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.RequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationAssemblyDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationDetailsDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.GetRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationStatusUpdate;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.UpdateTransformationRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.GraphStorageService;
import de.unistuttgart.stayinsync.core.configuration.rabbitmq.producer.TransformationJobMessageProducer;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.List;
import java.util.Optional;
import java.util.Set;


import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class TransformationService {

    @Inject
    TransformationMapper mapper;

    @Inject
    RequestConfigurationMapper requestConfigurationMapper;

    @Inject
    SyncJobService syncJobService;

    @Inject
    GraphStorageService graphStorageService;

    @Inject
    EntityManager entityManager;

    @Inject
    TransformationJobMessageProducer transformationMessageProducer;

    @Inject
    SourceSystemApiRequestConfigurationService sourceRequestConfigService;

    @Inject
    @Channel("transformation-status-updates")
    Emitter<TransformationStatusUpdate> statusEmitter;

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

        // NOTE: Previously collected SourceSystemEndpoints here; will be replaced with ARC configs when source ARCs are modeled

        transformation.syncJob = syncJobService.findSyncJobById(dto.syncJobId());
        transformation.transformationScript = script;
        transformation.transformationRule = rule;
        // NOTE: replace with api request configs
        //transformation.sourceSystemEndpoints = sourceEndpoints;

        if (script != null) {
            script.transformation = transformation;
        }

        if (rule != null) {
            rule.transformation = transformation;
        }

        return transformation;
    }

    public List<GetRequestConfigurationDTO> getTargetArcs(Long transformationId) {
        Log.debugf("Getting Target ARCs for Transformation with id %d", transformationId);

        Transformation transformation = Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Transformation not found", "Transformation with id %d not found.", transformationId));

        Set<TargetSystemApiRequestConfiguration> targetArcs = transformation.targetSystemApiRequestConfigurations;
        List<TargetSystemApiRequestConfiguration> targetArcsToList = targetArcs.stream().toList();
        return requestConfigurationMapper.mapToGetDTOList(targetArcsToList);
    }

    @Transactional
    public TransformationDetailsDTO updateTargetArcs(Long transformationId, UpdateTransformationRequestConfigurationDTO dto) {
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

        return mapper.mapToDetailsDTO(transformation);
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

    @Transactional(SUPPORTS)
    public List<Transformation> findAllBySyncjob(Long syncJobId) {
        Log.debug("Getting all transformations.");
        return Transformation.findBySyncJobId(syncJobId);
    }

    @Transactional(SUPPORTS)
    public List<Transformation> findAllWithSyncJobFilter(boolean withSyncJob) {
        if (withSyncJob) {
            Log.debug("Getting all transformations with sync job.");
            return Transformation.listAllWithSyncJob();
        }
        Log.debug("Getting all transformations without sync job.");
        return Transformation.listAllWithoutSyncJob();
    }

    public boolean delete(Long id) {
        Log.debugf("Deleting transformation with id %d", id);
        return Transformation.deleteById(id);
    }

    public void updateDeploymentStatus(Long transformationId, JobDeploymentStatus deploymentStatus) {
        Transformation transformation = findByIdDirect(transformationId);
        Log.infof("Settings deployment status of transformation with id %d to %s", transformationId, deploymentStatus);

        if (isTransitioning(transformation.deploymentStatus) && isTransitioning(deploymentStatus)) {
            Log.warnf("The transformation with id %d is currently in the deployment state of %s and thus can not be deployed or stopped", transformationId, transformation.deploymentStatus);
        } else {
            transformation.deploymentStatus = deploymentStatus;
            if (statusEmitter.hasRequests()) {
                statusEmitter.send(new TransformationStatusUpdate(transformationId, transformation.syncJob.id, deploymentStatus));
            }
            switch (deploymentStatus) {
                case DEPLOYING -> {
                    deployAssociatedRequestConfigs(transformation);
                    transformationMessageProducer.publishTransformationJob(mapper.mapToMessageDTO(transformation));
                }
                case STOPPING, RECONFIGURING -> {
                    deployAssociatedRequestConfigs(transformation);
                    sourceRequestConfigService.undeployAllUnused();
                    transformationMessageProducer.reconfigureDeployedTransformationJob(mapper.mapToMessageDTO(transformation));
                }
            }
            ;
        }
    }

    public void addRule(Long transformationId, Long ruleId) {
        TransformationRule ruleById = graphStorageService.findRuleById(ruleId);
        Transformation transformation = findByIdDirect(transformationId);
        Log.infof("Adding rule with id %d to transformation with id %d", ruleId, transformationId);

        removeRule(transformationId);
        transformation.transformationRule = ruleById;
        ruleById.transformation = transformation;
    }

    public void removeRule(Long transformationId) {
        Transformation transformation = findByIdDirect(transformationId);
        Log.infof("Removing rule from transformation with id %d", transformationId);
        if (transformation.transformationRule != null) {
            TransformationRule transformationRule = transformation.transformationRule;

            transformation.transformationRule.transformation = null;
            transformation.transformationRule = null;

            entityManager.flush();
        }
    }

    private void deployAssociatedRequestConfigs(Transformation transformation) {
        transformation.sourceSystemApiRequestConfigurations //
                .stream() //
                .filter(apiRequestConfiguration -> apiRequestConfiguration.deploymentStatus.equals(JobDeploymentStatus.UNDEPLOYED))
                .forEach(apiRequestConfiguration -> sourceRequestConfigService.updateDeploymentStatus(apiRequestConfiguration.id, JobDeploymentStatus.DEPLOYING));
    }

    private boolean isTransitioning(JobDeploymentStatus jobDeploymentStatus) {
        return Set.of(JobDeploymentStatus.DEPLOYING, JobDeploymentStatus.RECONFIGURING, JobDeploymentStatus.STOPPING).contains(jobDeploymentStatus);
    }

}
