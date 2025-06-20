package de.unistuttgart.stayinsync.core.configuration.domain.events.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;

public class SyncJobUpdatedEvent {
    public final SyncJob updatedSyncJob;
    public final SyncJob outdatedSyncJob;

    public SyncJobUpdatedEvent(SyncJob updatedSyncJob, SyncJob outdatedSyncJob) {
        this.updatedSyncJob = updatedSyncJob;
        this.outdatedSyncJob = outdatedSyncJob;
    }
}
