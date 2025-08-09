package de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Arrays;
import java.util.Map;
import de.unistuttgart.stayinsync.transport.exception.NodeConfigurationException;

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
     * @throws GraphEvaluationException if the {@code jsonPath} is malformed or if the
     * required data source is not found in the dataContext.
     */
    @Override
    public void calculate(Map<String, JsonNode> dataContext) throws GraphEvaluationException {
        String[] jsonPathKeys = jsonPath.split("\\.");

        if(!dataContext.containsKey("source")) {
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.DATA_NOT_FOUND,
                    "Invalid DataContext",
                    "Malformed dataContext, 'source' is not the first scoped key.",
                    null
            );
        }
        JsonNode sourceScope = dataContext.get("source");

        if(sourceScope == null) {
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.DATA_NOT_FOUND,
                    "Invalid DataContext",
                    "Malformed dataContext, no defined sourceSystemNames found under 'source'.",
                    null
            );
        }

        for (int i = 0; i < jsonPath.split("\\.").length; i++) {
            String key = jsonPathKeys[i];
            sourceScope = sourceScope.get(key);

            if (sourceScope == null) {
                throw new GraphEvaluationException(
                        GraphEvaluationException.ErrorType.DATA_NOT_FOUND,
                        "Path Not Found",
                        "Malformed dataContext, key '" + key + "' in path '" + jsonPath + "' does not exist.",
                        null
                );
            }
        }

        this.setCalculatedResult(sourceScope);
    }
}