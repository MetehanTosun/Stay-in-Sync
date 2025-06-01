package de.unistuttgart.stayinsync.pollingnode.entities;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PollingJob {

    private final Set<ApiAddress> sourceSystems;
    private FilterConfiguration filterConfiguration;
    private Map<String,Object> filteredData;
    private final boolean active;
    private SyncJob syncJob;

    public PollingJob(final Set<ApiAddress> sourceSystems, final FilterConfiguration filterConfiguration, final boolean active, final SyncJob syncJob){
        this.sourceSystems = sourceSystems;
        this.filterConfiguration = filterConfiguration;
        filteredData= new HashMap<>();
        this.active = active;
        this.syncJob = syncJob;

    }




}
