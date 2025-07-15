package de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Arrays;
import java.util.Map;

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
     * @throws IllegalArgumentException if jsonPath is null or empty.
     */
    public ProviderNode(String jsonPath) {
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            throw new IllegalArgumentException("jsonPath for ProviderNode cannot be null or empty.");
        }
        this.jsonPath = jsonPath;
    }

    /**
     * Calculates the value for this node by parsing its {@code jsonPath} and
     * extracting the corresponding value from the provided data context.
     * <p>
     * This method expects the {@code jsonPath} to follow a specific format:
     * {@code "source.{sourceName}.{path.within.source}"}. It parses this path
     * to determine the {@code sourceName}, which it uses as a key to retrieve the
     * correct root {@link JsonNode} from the dataContext. The remainder of the
     * path is then used to extract the final value from that {@code JsonNode}.
     * <p>
     * If the final path does not resolve to a value within the source JSON,
     * the node's calculated result will be set to {@code null}.
     *
     * @param dataContext A map where keys are logical source names and values are the
     * corresponding root {@link JsonNode} objects.
     * @throws IllegalStateException if the {@code jsonPath} is malformed or if the
     * required data source is not found in the dataContext.
     */
    @Override
    public void calculate(Map<String, JsonNode> dataContext) {
        String fullPath = this.getJsonPath();

        if (fullPath == null || !fullPath.startsWith("source.")) {
            throw new IllegalStateException("Invalid jsonPath format on node " + getId() + ": Must start with 'source.'");
        }

        // 1. Split the path into its components.
        String[] parts = fullPath.split("\\.");
        if (parts.length < 2) {
            throw new IllegalStateException("Invalid jsonPath format on node " + getId() + ": Must contain 'source.{sourceName}'.");
        }

        // 2. The 'sourceName' is always the second element and serves as the key for the context.
        String sourceName = parts[1];

        // 3. The 'internalJsonPath' is everything that follows.
        String internalJsonPath = (parts.length > 2) ? String.join(".", Arrays.copyOfRange(parts, 2, parts.length)) : "";

        // 4. Get the correct JSON object from the dataContext.
        JsonNode sourceObject = dataContext.get(sourceName);
        if (sourceObject == null) {
            throw new IllegalStateException("Data source '" + sourceName + "' for node " + getId() + " not found in dataContext.");
        }

        // 5. Extract the final value.
        JsonPathValueExtractor extractor = new JsonPathValueExtractor();
        Object result = extractor.extractValue(sourceObject, internalJsonPath)
                .orElse(null);

        this.setCalculatedResult(result);
    }
}