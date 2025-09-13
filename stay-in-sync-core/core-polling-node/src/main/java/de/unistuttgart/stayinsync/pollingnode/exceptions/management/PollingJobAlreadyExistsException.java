package de.unistuttgart.stayinsync.pollingnode.exceptions.management;


import de.unistuttgart.stayinsync.core.pollingnode.exceptions.PollingNodeException;

public class PollingJobAlreadyExistsException extends PollingNodeException {

    private Long idOfActivePollingJob;

    public PollingJobAlreadyExistsException(String message) {
        super(message);
    }

    public PollingJobAlreadyExistsException(String message, Long idOfActivePollingJob){
        super(message);
        this.idOfActivePollingJob = idOfActivePollingJob;
    }

    public Long getIdOfMessage(){
        if(idOfActivePollingJob == null){
            return -1L;
        }
        return idOfActivePollingJob;
    }
}
