package de.unistuttgart.stayinsync.pollingnode.exceptions.pollingjob.requestbuilderexceptions;

import de.unistuttgart.stayinsync.pollingnode.entities.RequestBuildingDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.pollingjob.PollingJobException;

public class RequestBuildingException extends PollingJobException {

    private RequestBuildingDetails requestBuildingDetails;

    public RequestBuildingException() {
        super();
    }

    public RequestBuildingException(String message) {
        super(message);
    }

    public RequestBuildingException(Throwable cause) {
        super(cause);
    }

    public RequestBuildingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestBuildingException(String message, Throwable cause, RequestBuildingDetails requestBuildingDetails) {
        super(message, cause);
        this.requestBuildingDetails = requestBuildingDetails;
    }

}
