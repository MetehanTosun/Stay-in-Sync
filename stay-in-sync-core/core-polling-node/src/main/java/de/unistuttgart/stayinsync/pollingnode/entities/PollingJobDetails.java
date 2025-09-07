package de.unistuttgart.stayinsync.pollingnode.entities;

public record PollingJobDetails(String name, Long id, int pollingIntervallTimeInMs,
                                RequestBuildingDetails requestBuildingDetails) {

}

