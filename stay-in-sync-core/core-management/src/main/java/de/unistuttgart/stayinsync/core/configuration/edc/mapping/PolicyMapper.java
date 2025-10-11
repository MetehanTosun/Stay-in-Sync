package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.PolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Policy;
import io.quarkus.logging.Log;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

public interface PolicyMapper {

    PolicyMapper mapper = Mappers.getMapper(PolicyMapper.class);

    @Mapping(target = "type", constant = "Policy")
    @Mapping(source = "contents", target = "contents", qualifiedByName = "stringToMap")
    PolicyDto entityToDto(Policy policy);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "policyDefinition", ignore = true)
    @Mapping(source = "contents", target = "contents", qualifiedByName = "mapToString")
    Policy dtoToEntity(PolicyDto policyDto);

    /**
     * Converts a JSON string to a Map for policy contents.
     * Parses the JSON string representation back to a Map<String, Object>.
     *
     * @param contentsString JSON string representation of policy contents
     * @return Map with policy contents
     */
    @Named("stringToMap")
    default Map<String, Object> stringToMap(String contentsString) {
        if (contentsString == null || contentsString.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(
                    contentsString,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            Log.error("Failed to deserialize policy contents from JSON string: " + contentsString, e);
            return new HashMap<>();
        }
    }

    /**
     * Converts a Map to JSON string for database storage.
     * Serializes the policy contents Map to a JSON string.
     *
     * @param contentsMap Map with policy contents
     * @return JSON string representation
     */
    @Named("mapToString")
    default String mapToString(Map<String, Object> contentsMap) {
        if (contentsMap == null || contentsMap.isEmpty()) {
            return "{}";
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(contentsMap);
        } catch (JsonProcessingException e) {
            Log.error("Failed to serialize policy contents to JSON string", e);
            return "{}";
        }
    }
}