package de.unistuttgart.stayinsync.pollingnode.entities;

public record PollingJobDetails(String name, Long id, int pollingIntervallTimeInMs, boolean active,
                                RequestBuildingDetails requestBuildingDetails) {

}

