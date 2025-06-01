package de.unistuttgart.stayinsync.pollingnode.entities;

import java.util.ArrayList;
import java.util.List;

public class FilterConfiguration {
    private final List<String> dataPoints;

    public FilterConfiguration(final SyncJob syncJob) {
        super();
        this.dataPoints = createFilterConfiguration(syncJob);
    }

    private List<String> createFilterConfiguration(final SyncJob syncJob){
        //TODO
    }
}
