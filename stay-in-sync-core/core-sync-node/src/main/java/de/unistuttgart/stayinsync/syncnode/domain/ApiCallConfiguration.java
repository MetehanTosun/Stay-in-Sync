package de.unistuttgart.stayinsync.syncnode.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class ApiCallConfiguration {
    private Map<String, Object> parameters;
    private Map<String, String> pathParameters;
    private JsonNode payload;

    public ApiCallConfiguration() {
    }

    public ApiCallConfiguration(Map<String, Object> parameters,
                                Map<String, String> pathParameters,
                                JsonNode payload) {
        this.parameters = parameters;
        this.pathParameters = pathParameters;
        this.payload = payload;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}