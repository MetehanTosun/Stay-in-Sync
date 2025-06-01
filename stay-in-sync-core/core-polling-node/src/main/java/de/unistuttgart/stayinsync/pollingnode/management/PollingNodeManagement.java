package de.unistuttgart.stayinsync.pollingnode.management;

import de.unistuttgart.stayinsync.pollingnode.configuration.PollingJobConfigurator;
import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import de.unistuttgart.stayinsync.pollingnode.service.PollingJobExecutionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PollingNodeManagement {

    private final PollingJobExecutionService pollingJobExecutionService;
    private final PollingJobConfigurator pollingJobConfigurator;

    @Inject
    public PollingNodeManagement(PollingJobExecutionService pollingJobExecutionService,
                                 PollingJobConfigurator pollingJobConfigurator) {
        super();
        this.pollingJobExecutionService = pollingJobExecutionService;
        this.pollingJobConfigurator = pollingJobConfigurator;
    }

    public void beginSupportOfSyncJob(final SyncJob syncJob){
        pollingJobConfigurator.createPollingJob();
    }

    public void editSupportedSyncJob(final SyncJob syncJob){
        pollingJobConfigurator.editPollingJob();
    }

    public void endSupportOfSyncJob(final SyncJob syncJob){
        pollingJobConfigurator.deletePollingJob();
    }

    public void accessPollingJobData(){

    }

}

