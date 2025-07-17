package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * A Data Transfer Object representing the static metadata for a single logic operator.
 * This is used to communicate an operator's "signature" to a client like a UI.
 */
@Getter
@Setter
public class OperatorMetadataDTO {

    /**
     * The unique name of the operator (e.g., "ADD", "EQUALS").
     */
    private String operatorName;

    /**
     * A human-readable description of what the operator does.
     */
    private String description;

    /**
     * A list of strings representing the expected data types for the operator's inputs.
     */
    private List<String> inputTypes;

    /**
     * A string representing the data type of the operator's output value.
     */
    private String outputType;

    /**
     * A static helper method to safely parse a JSON array string into a List<String>.
     *
     * @param json The JSON string from the database entity.
     * @return A List of strings, or an empty list if parsing fails or the input is null/blank.
     */
    public static List<String> fromJsonString(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // In case of a parsing error, return an empty list to prevent crashes.
            // A proper logger should be used here in a production environment.
            return Collections.emptyList();
        }
    }
}
