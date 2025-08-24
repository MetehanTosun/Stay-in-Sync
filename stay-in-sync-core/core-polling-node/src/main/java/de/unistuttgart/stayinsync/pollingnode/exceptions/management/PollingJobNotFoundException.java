package de.unistuttgart.stayinsync.pollingnode.exceptions.management;

public class PollingJobNotFoundException extends Exception {

    private Long idOfMessageForNonExistingPollingJob;

    public PollingJobNotFoundException(String message) {
        super(message);
    }

    public PollingJobNotFoundException(String message, Long id) {
        super(message);
        this.idOfMessageForNonExistingPollingJob = id;
    }

    public Long getIdOfMessageForNonExistingPollingJob(){
        if(idOfMessageForNonExistingPollingJob == null){
            return -1L;
        }
        return idOfMessageForNonExistingPollingJob;
    }
}
