package de.unistuttgart.stayinsync.pollingnode.exceptions.pollingjob.requestbuilderexceptions;

import de.unistuttgart.stayinsync.pollingnode.exceptions.pollingjob.PollingJobException;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemEndpointMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemMessageDTO;

public class RequestBuildingDetailsNullFieldException extends PollingJobException {

    private SourceSystemMessageDTO sourceSystem;
    private SourceSystemEndpointMessageDTO endpoint;

    public RequestBuildingDetailsNullFieldException(String message) {
        super(message);
    }

    public RequestBuildingDetailsNullFieldException(String message, SourceSystemMessageDTO sourceSystem) {
        super(message);
        this.sourceSystem = sourceSystem;
    }

    public RequestBuildingDetailsNullFieldException(String message, SourceSystemMessageDTO sourceSystem, SourceSystemEndpointMessageDTO endpoint) {
        super(message);
        this.sourceSystem = sourceSystem;
        this.endpoint = endpoint;
    }
}
