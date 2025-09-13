package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SyncJobFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobCreationDTO;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class SyncJobService {

    @Inject
    Validator validator;

    @Inject
    TransformationService transformationService;

    @Inject
    SyncJobFullUpdateMapper syncJobFullUpdateMapper;


    public SyncJob persistSyncJob(@NotNull @Valid SyncJobCreationDTO syncJobDTO) {
        SyncJob syncJob = syncJobFullUpdateMapper.mapToEntity(syncJobDTO);

        List<Transformation> transformations = syncJobDTO.transformationIds().stream()
                .map(transformationId -> {
                    Transformation transformation = transformationService.findByIdDirect(transformationId);
                    transformation.syncJob = syncJob;
                    return transformation;
                }).toList();

        syncJob.transformations.addAll(transformations);

        Log.debugf("Persisting sync-job: %s", syncJob);

        syncJob.persist();

        return syncJob;
    }


    @Transactional(SUPPORTS)
    public List<SyncJob> findAllSyncJobsHavingName(String name) {
        Log.debugf("Finding all sync-job having name = %s", name);
        return Optional.ofNullable(SyncJob.listAllWhereNameLike(name))
                .orElseGet(List::of);
    }

    @Transactional(SUPPORTS)
    public List<SyncJob> findAllSyncJobs() {
        Log.debug("Getting all sync-jobs");
        return Optional.ofNullable(SyncJob.<SyncJob>listAll())
                .orElseGet(List::of);
    }


    @Transactional(SUPPORTS)
    public SyncJob findSyncJobById(Long id) {
        Log.debugf("Finding sync-job by id = %d", id);
        return SyncJob.findByIdOptional(id)
                .map(SyncJob.class::cast)
                .orElseThrow(() -> {
                    Log.warnf("There is no sync-job with id: %d", id);
                    throw new CoreManagementException("Unable to find Sync-job", "There is no sync-job with id: %d", id);
                });
    }


    @Transactional(REQUIRED)
    public void deleteSyncJob(Long id) {
        Log.debugf("Deleting sync-job by id = %d", id);

        SyncJob syncJob = SyncJob.findById(id);
        if (syncJob != null) {
            // Setze die syncJobId der Transformationen auf null
            if (syncJob.transformations != null && !syncJob.transformations.isEmpty()) {
                syncJob.transformations.forEach(transformation -> {
                    transformation.syncJob = null;
                    transformation.persist();
                });
            }

            // Löschen des SyncJob
            syncJob.delete();
            Log.debugf("Sync-job with id %d deleted", id);
        } else {
            Log.warnf("Sync-job with id %d not found", id);
        }
    }

    public void addTransformation(Long id, Long transformationId) {
        Log.infof("Adding transformation with id %d to syncjob with id %d", transformationId, id);
        SyncJob syncJobById = findSyncJobById(id);
        Transformation transformation = transformationService.findByIdDirect(transformationId);
        transformation.syncJob = syncJobById;
        syncJobById.transformations.add(transformation);
    }

    public void removeTransformation(Long id, Long transformationId) {
        Log.infof("Removing transformation with id %d to syncjob with id %d", transformationId, id);
        SyncJob syncJobById = findSyncJobById(id);
        Transformation transformation = transformationService.findByIdDirect(transformationId);
        transformation.syncJob = null;
        syncJobById.transformations.remove(transformation);
    }


    @Transactional(REQUIRED)
    public SyncJob replaceSyncJob(@NotNull @Valid SyncJobCreationDTO syncJobDTO) {
        SyncJob syncJob = syncJobFullUpdateMapper.mapToEntity(syncJobDTO);

        List<Transformation> transformations = syncJobDTO.transformationIds().stream()
                .map(transformationId -> {
                    Transformation transformation = transformationService.findByIdDirect(transformationId);
                    transformation.syncJob = syncJob;
                    return transformation;
                }).toList();

        syncJob.transformations.clear();
        syncJob.transformations.addAll(transformations);

        return SyncJob.findByIdOptional(syncJob.id)
                .map(SyncJob.class::cast)
                .map(targetSyncJob -> {
                    this.syncJobFullUpdateMapper.mapFullUpdate(syncJob, targetSyncJob);
                    return targetSyncJob;
                })
                .orElseThrow(() -> new IllegalArgumentException("SyncJob with ID " + syncJob.id + " not found"));
    }
}
