package de.unistuttgart.graphengine.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;


/**
 * A node that provides a value by extracting it from an external JSON data source
 * at runtime. It serves as a dynamic input for the graph.
 */
@Getter
@Setter
@NoArgsConstructor // Important for mappers/frameworks
public class ProviderNode extends Node {

    /**
     * An optional ID for identifying the source system or component (e.g., an ARC id).
     */
    private Integer arcId;

    /**
     * The full semantic path to the data source, e.g., "source.anlageAAS.sensorData.temp".
     */
    private String jsonPath;

    /**
     * Constructs a new ProviderNode with a specific jsonPath.
     *
     * @param jsonPath The full semantic path to the data source. Cannot be null or empty.
     * @throws NodeConfigurationException if jsonPath is null or empty.
     */
    public ProviderNode(String jsonPath) throws NodeConfigurationException {
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            throw new NodeConfigurationException("jsonPath for ProviderNode cannot be null or empty.");
        }

        if(!jsonPath.startsWith("source")){
            throw new NodeConfigurationException("jsonPath for ProviderNode must start with 'source'.");
        }

        String[] parts = jsonPath.split("\\.");
        if (parts.length < 2) {
            throw new NodeConfigurationException("Invalid jsonPath format on node " + getId() + ": Must contain 'source.{sourceName}'.");
        }

        this.jsonPath = jsonPath;
    }

    /**
     * Calculates the value for this node by extracting it from the data context.
     * <p>
     * This method expects the {@code dataContext} to contain a {@link JsonNode} under the key "source".
     * It then uses the node's {@code jsonPath} (e.g., "source.sensor.temperature") to navigate
     * within this {@code JsonNode} to find and extract the final value.
     * <p>
     * If the path does not resolve to a value, the node's calculated result is set to {@code null}.
     *
     * @param dataContext A map where keys are logical names (like "source") and values are the
     * corresponding data objects.
     * @throws GraphEvaluationException if the "source" key is missing from the dataContext or if the
     * corresponding value is not a {@link JsonNode}.
     */
    @Override
    public void calculate(Map<String, Object> dataContext) throws GraphEvaluationException {

        // Safely get the source data object from the context
        Object sourceObject = dataContext.get("source");

        if (sourceObject == null) {
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.DATA_NOT_FOUND,
                    "DataContext Missing 'source'",
                    "The dataContext must contain a non-null entry for the key 'source'.",
                    null
            );
        }

        // Check the type before casting to prevent ClassCastException
        if (!(sourceObject instanceof JsonNode)) {
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.TYPE_MISMATCH,
                    "Invalid DataContext Type",
                    "The value for 'source' in dataContext must be a JsonNode, but was " + sourceObject.getClass().getName(),
                    null
            );
        }

        JsonNode sourceScope = (JsonNode) sourceObject;

        // Create the internal path (without the first "source" part)
        String[] jsonPathKeys = jsonPath.split("\\.");
        String[] remainingPathParts = Arrays.copyOfRange(jsonPathKeys, 1, jsonPathKeys.length);
        String internalPath = String.join(".", remainingPathParts);

        // Use JsonPathValueExtractor for navigation and conversion
        JsonPathValueExtractor extractor = new JsonPathValueExtractor();
        Optional<Object> result = extractor.extractValue(sourceScope, internalPath);

        this.setCalculatedResult(result.orElse(null));
    }


    @Override
    public Class<?> getOutputType() {
        return Object.class;
    }
}