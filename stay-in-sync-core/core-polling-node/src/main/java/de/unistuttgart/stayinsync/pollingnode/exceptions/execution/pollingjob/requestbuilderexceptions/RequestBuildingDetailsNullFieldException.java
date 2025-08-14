package de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions;

import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.PollingJobException;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemEndpointMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemMessageDTO;

public class RequestBuildingDetailsNullFieldException extends PollingJobException {

    private String sourceSystemName;

    public RequestBuildingDetailsNullFieldException(String message) {
        super(message);
    }

    public RequestBuildingDetailsNullFieldException(String message, String sourceSystemName) {
        super(message);
        this.sourceSystemName = sourceSystemName;
    }

    public String getSourceSystemName(){
        if(sourceSystemName == null){
            return "No SourceSystemName defined";
        }
        return sourceSystemName;
    }

}
