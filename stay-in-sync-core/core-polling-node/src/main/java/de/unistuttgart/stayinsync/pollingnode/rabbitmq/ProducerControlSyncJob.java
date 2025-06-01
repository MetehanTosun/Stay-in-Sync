package de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import de.unistuttgart.stayinsync.pollingnode.management.PollingNodeManagement;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProducerControlSyncJob {

    private final PollingNodeManagement pollingNodeManagement;

    public ProducerControlSyncJob(final PollingNodeManagement pollingNodeManagement){
        this.pollingNodeManagement = pollingNodeManagement;
    }

    public void reactToMessage(){
        if(){
            syncJobSupportRequest();
        } else if () {
            supportedSyncJobUpdateRequest();
        } else{
            supportedSyncJobDeletionRequest();
        }
    }

    private void syncJobSupportRequest(final SyncJob syncJob) {
        pollingNodeManagement.beginSupportOfSyncJob(syncJob);
    }

    private void supportedSyncJobUpdateRequest(final SyncJob syncJob){
        pollingNodeManagement.editSupportedSyncJob(syncJob);
    }

    /**
     * PollingNodeManagement activation to respond to SyncJob Deletion Request
     */
    private void supportedSyncJobDeletionRequest(final SyncJob syncJob){
        pollingNodeManagement.endSupportOfSyncJob(syncJob);
    }
}
