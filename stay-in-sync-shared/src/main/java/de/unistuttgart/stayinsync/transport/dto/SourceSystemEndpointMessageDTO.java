package de.unistuttgart.stayinsync.transport.dto;


public class SourceSystemEndpointMessageDTO {
    private String endpointPath;
    private final String httpRequestType;

    public SourceSystemEndpointMessageDTO(String endpointPath, String httpRequestType) {
        this.endpointPath = endpointPath;
        this.httpRequestType = httpRequestType;
    }

    public String endpointPath() {
        return endpointPath;
    }

    public String httpRequestType() {
        return httpRequestType;
    }

    public void insertPathParametersIntoEndpointPath(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage){
        String resultEndpointPath = apiRequestConfigurationMessage.apiConnectionDetails().endpoint().endpointPath();
        for(ApiRequestParameterMessageDTO parameter: apiRequestConfigurationMessage.apiConnectionDetails().requestParameters()){
            if(parameter.type() == ParamType.PATH){
                resultEndpointPath = resultEndpointPath.replace("{" + parameter.paramName() + "}", "{" + parameter.paramValue() + "}");
            }
        }
        this.endpointPath = resultEndpointPath;
    }

}