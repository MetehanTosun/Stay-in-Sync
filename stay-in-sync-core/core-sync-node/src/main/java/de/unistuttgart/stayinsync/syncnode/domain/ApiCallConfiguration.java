package de.unistuttgart.stayinsync.syncnode.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

@Data
public class ApiCallConfiguration {
    private Map<String, Object> parameters;
    private Map<String, String> pathParameters;
    private JsonNode payload;
}
