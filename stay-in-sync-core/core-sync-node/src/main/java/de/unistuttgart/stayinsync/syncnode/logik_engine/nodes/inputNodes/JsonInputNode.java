package de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Represents an input node that sources its value from a specific path
 * within an external JSON object (typically representing AAS data).
 * The source JsonObject and path are defined at creation.
 */
@Getter
@Setter
public class JsonInputNode implements InputNode {
    private final String jsonPath;
    private final String sourceName;
    private final JsonPathValueExtractor valueExtractor = new JsonPathValueExtractor();

    /**
     * Constructs a new JsonInputNode, which acts as a placeholder for a value to be retrieved
     * from a named JSON data source at runtime.
     *
     * @param sourceName The logical name of the data source (e.g., "anlageAAS", "wetterAPI").
     *                   This name is used at evaluation time to look up the correct {@link JsonNode}
     *                   from the provided data context map. Cannot be null or empty.
     * @param path   The dot-separated path to the value within the source JsonObject.
     *               Cannot be null or empty.
     * @throws IllegalArgumentException if source is null, or if path is null or empty.
     */
    public JsonInputNode(String sourceName, String path) {
        if (sourceName == null || sourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Source name for JsonInputNode cannot be null or empty.");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON path for JsonInputNode cannot be null or empty.");
        }
        this.sourceName = sourceName;
        this.jsonPath = path;
    }

    /**
     * Retrieves the value from the source {@link JsonNode} using the configured path.
     * This method relies on the {@link JsonPathValueExtractor} to navigate the Jackson object tree.
     *
     * @param context The runtime data context containing the source JsonNodes.
     * @return The extracted Java object (e.g., String, Long, Double, Boolean).
     * @throws IllegalStateException if the path does not resolve to a value or if the data source is missing.
     */
    @Override
    public Object getValue(Map<String, JsonNode> context) {
        if (context == null) {
            throw new IllegalStateException("Data context is null, but JsonInputNode requires it to resolve a value.");
        }

        JsonNode sourceObject = context.get(this.sourceName);

        return valueExtractor.extractValue(sourceObject, this.jsonPath)
                .orElseThrow(() -> new IllegalStateException(
                        "Value for path '" + this.jsonPath + "' not found in data source '" + this.sourceName + "'."
                ));
    }

    /**
     * @return false, as this provider is not sourced from a LogicNode.
     */
    @Override
    public boolean isParentNode() {
        return false;
    }

    /**
     * @return true, as this input's value comes from a JSON object.
     */
    @Override
    public boolean isJsonInputNode() {
        return true;
    }

    /**
     * @return false, as this provider is not sourced from a ConstantNode.
     */
    @Override
    public boolean isConstantNode() {
        return false;
    }

    /**
     * Retrieves the parent LogicNode. Not applicable for JsonInputNode.
     * @throws UnsupportedOperationException always, as JsonInputNode does not have a parent LogicNode.
     */
    @Override
    public LogicNode getParentNode() {
        // Corrected exception message to reflect JsonInputNode (or a more generic message)
        throw new UnsupportedOperationException("JsonInputNode does not have a parent LogicNode.");
    }
}