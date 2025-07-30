package de.unistuttgart.stayinsync.pollingnode.exceptions.management;

public class PollingJobNotFoundException extends Exception {

    Long id;

    public PollingJobNotFoundException(String message) {
        super(message);
    }

    public PollingJobNotFoundException(String message, Long id) {
        super(message);
        this.id = id;
    }
}
