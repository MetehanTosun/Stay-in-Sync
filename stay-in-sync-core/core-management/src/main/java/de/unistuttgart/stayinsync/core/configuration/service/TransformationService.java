package de.unistuttgart.stayinsync.core.configuration.service;


import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationMapper;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationAssemblyDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationStatusUpdate;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.UpdateTransformationRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.GetTypeDefinitionsResponseDTO;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.GraphStorageService;
import de.unistuttgart.stayinsync.core.configuration.messaging.producer.TransformationJobMessageProducer;
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
    SyncJobService syncJobService;

    @Inject
    GraphStorageService graphStorageService;

    @Inject
    TargetDtsBuilderGeneratorService targetDtsBuilderGeneratorService;

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

    /**
     * Updates the Many-to-Many relationship between a Transformation and its Target ARCs.
     * This method provides a transactional, atomic update by clearing all existing target ARC associations
     * and re-linking them based on the provided lists of IDs. After successfully persisting the new
     * relationships, it regenerates and returns the corresponding TypeScript Declaration files (.d.ts)
     * for the Monaco editor.
     *
     * @param transformationId The ID of the Transformation to update.
     * @param dto              The {@link UpdateTransformationRequestConfigurationDTO} containing the complete lists of REST and AAS Target ARC IDs to be linked.
     * @return A {@link GetTypeDefinitionsResponseDTO} containing the newly generated TypeScript libraries reflecting the updated configuration.
     * @throws CoreManagementException if the Transformation or any of the specified ARC IDs cannot be found.
     */
    @Transactional
    public GetTypeDefinitionsResponseDTO updateTargetArcs(Long transformationId, UpdateTransformationRequestConfigurationDTO dto) {
        Log.debugf("Updating ALL Target ARCs for Transformation with id %d", transformationId);

        Transformation transformation = findTransformationOrThrow(transformationId);

        // Clear existing associations before re-linking.
        transformation.targetSystemApiRequestConfigurations.clear();
        transformation.aasTargetApiRequestConfigurations.clear();

        // Bind the new sets of ARCs.
        bindRestTargetArcs(transformation, dto.restTargetArcIds());
        bindAasTargetArcs(transformation, dto.aasTargetArcIds());

        transformation.persist();
        entityManager.flush(); // Ensure changes are written to the DB before DTS generation.

        Log.infof("Successfully updated Target ARCs for Transformation %d. REST ARC count: %d, AAS ARC count: %d",
                transformationId,
                transformation.targetSystemApiRequestConfigurations.size(),
                transformation.aasTargetApiRequestConfigurations.size());

        return targetDtsBuilderGeneratorService.generateForTransformation(transformationId);
    }

    /**
     * Finds a Transformation by its ID or throws a NOT_FOUND exception if it doesn't exist.
     *
     * @param transformationId The ID of the transformation to find.
     * @return The found {@link Transformation} entity.
     * @throws CoreManagementException if the transformation is not found.
     */
    private Transformation findTransformationOrThrow(Long transformationId) {
        return Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Transformation not found", "Transformation with id %d not found.", transformationId));
    }

    /**
     * Finds and links a list of REST Target ARCs to a Transformation.
     *
     * @param transformation The parent {@link Transformation} entity.
     * @param restArcIds     The list of REST Target ARC IDs to link.
     * @throws CoreManagementException if any of the provided IDs do not correspond to an existing entity.
     */
    private void bindRestTargetArcs(Transformation transformation, Set<Long> restArcIds) {
        if (restArcIds == null || restArcIds.isEmpty()) {
            Log.info("No REST Target ARCs to bind.");
            return;
        }

        List<TargetSystemApiRequestConfiguration> arcsToLink = TargetSystemApiRequestConfiguration.list("id in ?1", restArcIds);
        validateArcCount(arcsToLink.size(), restArcIds.size(), "REST");

        transformation.targetSystemApiRequestConfigurations.addAll(arcsToLink);
        // Also update the inverse side of the relationship for proper JPA state management.
        arcsToLink.forEach(arc -> arc.transformations.add(transformation));
    }

    /**
     * Finds and links a list of AAS Target ARCs to a Transformation.
     *
     * @param transformation The parent {@link Transformation} entity.
     * @param aasArcIds      The list of AAS Target ARC IDs to link.
     * @throws CoreManagementException if any of the provided IDs do not correspond to an existing entity.
     */
    private void bindAasTargetArcs(Transformation transformation, Set<Long> aasArcIds) {
        if (aasArcIds == null || aasArcIds.isEmpty()) {
            Log.info("No AAS Target ARCs to bind.");
            return;
        }

        List<AasTargetApiRequestConfiguration> arcsToLink = AasTargetApiRequestConfiguration.list("id in ?1", aasArcIds);
        validateArcCount(arcsToLink.size(), aasArcIds.size(), "AAS");

        transformation.aasTargetApiRequestConfigurations.addAll(arcsToLink);
        // Also update the inverse side of the relationship for proper JPA state management.
        arcsToLink.forEach(arc -> arc.transformations.add(transformation));
    }

    /**
     * A utility method to validate that the number of ARCs found in the database matches the number of IDs requested.
     *
     * @param foundCount     The number of entities retrieved from the database.
     * @param requestedCount The number of IDs provided in the request DTO.
     * @param arcType        A string representing the ARC type (e.g., "REST", "AAS") for use in the error message.
     * @throws CoreManagementException if the counts do not match.
     */
    private void validateArcCount(int foundCount, int requestedCount, String arcType) {
        if (foundCount != requestedCount) {
            throw new CoreManagementException(
                    Response.Status.BAD_REQUEST,
                    String.format("Invalid %s ARC ID", arcType),
                    String.format("One or more provided %s Target ARC IDs could not be found.", arcType)
            );
        }
    }

    @Transactional(SUPPORTS)
    public Optional<Transformation> findById(Long id) {
        Log.debugf("Finding transformation with id %d", id);
        return Transformation.findByIdOptional(id);
    }

    @Transactional(SUPPORTS)
    public Transformation findByIdDirect(Long id) {
        Log.debugf("Finding transformation with id %d", id);
        Transformation transformation = Transformation.findById(id);

        if (transformation == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "Transformation not found", "No Transformation found using id %d", id);
        }

        return transformation;
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

    public void updateDeploymentStatus(Long transformationId, JobDeploymentStatus deploymentStatus, String hostName) {
        Transformation transformation = findByIdDirect(transformationId);
        Log.infof("Settings deployment status of transformation with id %d to %s", transformationId, deploymentStatus);

        if (isTransitioning(transformation.deploymentStatus) && isTransitioning(deploymentStatus)) {
            Log.warnf("The transformation with id %d is currently in the deployment state of %s and thus can not be deployed or stopped", transformationId, transformation.deploymentStatus);
        } else {
            transformation.deploymentStatus = deploymentStatus;
            transformation.workerHostName = hostName;

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
                .forEach(apiRequestConfiguration -> sourceRequestConfigService.updateDeploymentStatus(apiRequestConfiguration.id, JobDeploymentStatus.DEPLOYING, null));
    }

    private boolean isTransitioning(JobDeploymentStatus jobDeploymentStatus) {
        return Set.of(JobDeploymentStatus.DEPLOYING, JobDeploymentStatus.RECONFIGURING, JobDeploymentStatus.STOPPING).contains(jobDeploymentStatus);
    }

}
