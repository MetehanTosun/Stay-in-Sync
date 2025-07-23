package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.List;

/**
 * A Data Transfer Object representing the static metadata for a single logic operator.
 * This is used to communicate an operator's "signature" to a client like a UI.
 *
 * @param operatorName The unique name of the operator (e.g., "ADD", "EQUALS").
 * @param description  A human-readable description of what the operator does.
 * @param inputTypes   A list of strings representing the expected data types for the operator's inputs.
 * @param outputType   A string representing the data type of the operator's output value.
 */
public record OperatorMetadataDTO(
        String operatorName,
        String description,
        String category,
        List<String> inputTypes,
        String outputType
) {
}
