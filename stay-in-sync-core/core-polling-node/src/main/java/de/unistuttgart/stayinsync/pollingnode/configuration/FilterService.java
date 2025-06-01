package de.unistuttgart.stayinsync.pollingnode.configuration;

import de.unistuttgart.stayinsync.pollingnode.entities.FilterConfiguration;
import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import jakarta.enterprise.context.ApplicationScoped;


/**
 * Used by the PollingJobExecutionService
 * Updates PollingJobs with filteredData based on their FilterConfiguration and SourceSystems.
 */
@ApplicationScoped
public class FilterService {
    public FilterService(){
        super();
    }


}
