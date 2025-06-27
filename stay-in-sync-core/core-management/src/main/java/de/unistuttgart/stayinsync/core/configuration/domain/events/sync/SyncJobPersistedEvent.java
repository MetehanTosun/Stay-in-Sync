package de.unistuttgart.stayinsync.core.configuration.domain.events.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;

public class SyncJobPersistedEvent {

    public final SyncJob newSyncJob;

    public SyncJobPersistedEvent(SyncJob newSyncJob) {
        this.newSyncJob = newSyncJob;
    }
}
