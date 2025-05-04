package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.mapping.SyncJobFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.SyncJob;
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
    SyncJobFullUpdateMapper syncJobFullUpdateMapper;

    public SyncJob persistSyncJob(@NotNull @Valid SyncJob syncJob) {
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
    public Optional<SyncJob> findSyncJobById(Long id) {
        Log.debugf("Finding sync-job by id = %d", id);
        return SyncJob.findByIdOptional(id);
    }

    public void deleteSyncJob(Long id) {
        Log.debugf("Deleting sync-job by id = %d", id);
        SyncJob.deleteById(id);
    }

    public Optional<SyncJob> replaceSyncJob(@NotNull @Valid SyncJob syncJob) {
        Log.debugf("Replacing sync-job: %s", syncJob);

        return SyncJob.findByIdOptional(syncJob.id)
                .map(SyncJob.class::cast) // Only here for type erasure within the IDE
                .map(targetSyncJob -> {
                    this.syncJobFullUpdateMapper.mapFullUpdate(syncJob, targetSyncJob);
                    return targetSyncJob;
                });
    }


}
