package de.unistuttgart.stayinsync.monitoring.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobPersistedEvent;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobUpdatedEvent;
import de.unistuttgart.stayinsync.core.configuration.mapping.SyncJobFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobCreationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationService;
import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Inject
    Event<SyncJobPersistedEvent> syncJobPersistedEvent;

    @Inject
    Event<SyncJobUpdatedEvent> syncJobUpdatedEventEvent;

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
        syncJobPersistedEvent.fire(new SyncJobPersistedEvent(syncJob));

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
    public Optional<SyncJob> findSyncJobById(Long id) {
        Log.debugf("Finding sync-job by id = %d", id);
        return SyncJob.findByIdOptional(id);
    }

    public void deleteSyncJob(Long id) {
        Log.debugf("Deleting sync-job by id = %d", id);
        SyncJob.deleteById(id);
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
                    syncJobUpdatedEventEvent.fire(new SyncJobUpdatedEvent(targetSyncJob, syncJob));
                    return targetSyncJob;
                })
                .orElseThrow(() -> new IllegalArgumentException("SyncJob with ID " + syncJob.id + " not found"));
    }



}
