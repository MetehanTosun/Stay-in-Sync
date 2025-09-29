package de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions;

import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.PollingJobException;

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
