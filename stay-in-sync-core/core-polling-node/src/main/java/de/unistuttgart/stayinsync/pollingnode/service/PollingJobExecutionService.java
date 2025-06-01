package de.unistuttgart.stayinsync.pollingnode.service;

import de.unistuttgart.stayinsync.pollingnode.configuration.PollingJobConfigurator;
import de.unistuttgart.stayinsync.pollingnode.entities.ApiAddress;
import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.ressource.RestRessource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PollingJobExecutionService {
    private final PollingJobConfigurator pollingJobConfigurator;
    private final RestRessource restRessource;
    private final Map<ApiAddress, Json> cachedJsonValues;
    private final Map<ApiAddress, List<PollingJob>> pollingNodesThatUseApiAddress;


    public PollingJobExecutionService(PollingJobConfigurator pollingJobConfigurator, RestRessource restRessource){
        super();
        this.pollingJobConfigurator = pollingJobConfigurator;
        this.restRessource = restRessource;
        this.cachedJsonValues = new HashMap<>();
        this.pollingNodesThatUseApiAddress = new HashMap<>();
    }

    public Json getCachedJsonOf(final ApiAddress apiAddress){

    }

    private void updateCachedJsonOf(final ApiAddress apiAddress){
        Json json = restRessource.getJsonDataOf(apiAddress);
        cachedJsonValues.put(apiAddress, json);
    }

    private void removeCacheOf(final ApiAddress apiAddress){

        restRessource.removeCachedHttpRequest(apiAddress);
    }


}
