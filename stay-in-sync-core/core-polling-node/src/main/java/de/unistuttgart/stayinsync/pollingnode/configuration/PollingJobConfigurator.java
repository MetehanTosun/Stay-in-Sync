package de.unistuttgart.stayinsync.pollingnode.configuration;

import de.unistuttgart.stayinsync.pollingnode.entities.ApiAddress;
import de.unistuttgart.stayinsync.pollingnode.entities.FilterConfiguration;
import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Manages all PollingJobs during their whole Lifespan.
 * Keeps PollingJobs updates based on schedule timing.
 */

@ApplicationScoped
public class PollingJobConfigurator {

    private final List<PollingJob> pollingJobs;

    public PollingJobConfigurator(final FilterService filterConfigurator){
        super();
        this.pollingJobs = new ArrayList<>();
    }

    /**
     * Creates PollingJob based on information of SyncJob
     * @param syncJob used to get important initialisation information. Also saved as attribute of new PollingJob
     */
    public void createPollingJob(final SyncJob syncJob){
        Set<ApiAddress> sourceSystems = this.extractApiAddresses(syncJob);
        FilterConfiguration filterConfiguration = new FilterConfiguration(syncJob);
        boolean active = this.checkActiveSettingSyncJob();
        pollingJobs.add(new PollingJob(sourceSystems, filterConfiguration, active, syncJob));


    }


    /*
     * Extracts active boolean from SyncJob
     * @return boolean activityState of SyncJob
     */
    private boolean checkActiveSettingSyncJob(){
        //TODO
    }

    /*
    * Extracts the Source System ApiAddresses of the SyncNode
    * @return HashSet of ApiAddresses
     */
    private HashSet<ApiAddress> extractApiAddresses(final SyncJob syncJob){
        //TODO
    }
}
