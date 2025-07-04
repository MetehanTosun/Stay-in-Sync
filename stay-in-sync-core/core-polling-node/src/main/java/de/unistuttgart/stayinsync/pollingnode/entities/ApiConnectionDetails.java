package de.unistuttgart.stayinsync.pollingnode.entities;

import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemEndpointMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemMessageDTO;

import java.util.Objects;
import java.util.Set;

public record ApiConnectionDetails(SourceSystemMessageDTO sourceSystem, SourceSystemEndpointMessageDTO endpoint,  Set<ApiRequestParameterMessageDTO> requestParameters,
                                   Set<ApiRequestHeaderMessageDTO> requestHeader) {

    public boolean allFieldsInCorrectFormat(){
        return allFieldsNotNull() && allImportantStringsNotEmpty();
    }

    private boolean allFieldsNotNull(){
        if(sourceSystem == null
                || sourceSystem.apiUrl() == null
                || sourceSystem.apiType() == null
                || sourceSystem.authDetails() == null
                || sourceSystem.authDetails().apiKey == null
                || sourceSystem.authDetails().headerName == null){
            return false;
        }
        if(endpoint == null || endpoint.endpointPath() == null || endpoint.httpRequestType() == null){
            return false;
        }
        if(requestParameters == null){
            return false;
        }
        if(requestHeader == null){
            return false;
        }
        return true;
    }

    private boolean allImportantStringsNotEmpty(){
        return !Objects.equals(sourceSystem.apiUrl(), "") && !Objects.equals(endpoint.endpointPath(),"");
    }

}
