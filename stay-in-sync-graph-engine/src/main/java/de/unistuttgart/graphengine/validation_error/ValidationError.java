package de.unistuttgart.graphengine.validation_error;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An interface representing a single validation failure in a logic graph.
 * It provides a user-friendly message and an error code for the frontend.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "errorCode"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OperatorConfigurationError.class, name = "OPERATOR_CONFIG_ERROR"),
        @JsonSubTypes.Type(value = CycleError.class, name = "CYCLE_DETECTED"),
        @JsonSubTypes.Type(value = FinalNodeError.class, name = "FINAL_NODE_ERROR"),
        @JsonSubTypes.Type(value = NodeConfigurationError.class, name = "NODE_CONFIG_ERROR"),
        @JsonSubTypes.Type(value = ConfigNodeError.class, name = "CONFIG_NODE_ERROR")

})
public interface ValidationError {

    /**
     * @return A machine-readable error code (e.g., "CYCLE_DETECTED").
     */
    String getErrorCode();

    /**
     * @return A human-readable message describing the error.
     */
    String getMessage();
}
