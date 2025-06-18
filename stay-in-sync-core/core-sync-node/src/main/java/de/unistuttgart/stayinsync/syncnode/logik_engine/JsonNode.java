package de.unistuttgart.stayinsync.syncnode.logik_engine;

import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Optional;

/**
 * Represents an input node that sources its value from a specific path
 * within an external JSON object (typically representing AAS data).
 * The source JsonObject and path are defined at creation.
 */
@Getter
@Setter
public class JsonNode implements InputNode {
    private final String jsonPath;
    private final String sourceName;
    private final JsonObjectValueExtractor valueExtractor = new JsonObjectValueExtractor();

    /**
     * Constructs a new JsonNode, which acts as a placeholder for a value to be retrieved
     * from a named JSON data source at runtime.
     *
     * @param sourceName The logical name of the data source (e.g., "anlageAAS", "wetterAPI").
     *                   This name is used at evaluation time to look up the correct {@link JsonObject}
     *                   from the provided data context map. Cannot be null or empty.
     * @param path   The dot-separated path to the value within the source JsonObject.
     *               Cannot be null or empty.
     * @throws IllegalArgumentException if source is null, or if path is null or empty.
     */
    public JsonNode(String sourceName, String path) {
        if (sourceName == null || sourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Source name for JsonNode cannot be null or empty.");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON path for JsonNode cannot be null or empty.");
        }
        this.sourceName = sourceName;
        this.jsonPath = path;
    }

    /**
     * Retrieves the value from the source JsonObject using the configured jsonPath.
     *
     * @return The extracted value as an Object.
     * @throws IllegalStateException if the path does not resolve to a value or if the value is JSON null.
     */
    @Override
    public Object getValue(Map<String, JsonObject> context) {
        if (context == null) {
            throw new IllegalStateException("Data context is null, but JsonNode requires it to resolve a value.");
        }

        JsonObject sourceObject = context.get(this.sourceName);
        if (sourceObject == null) {
            throw new IllegalStateException(
                    "Data source '" + this.sourceName + "' required by JsonNode was not provided in the runtime data context."
            );
        }

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
    public boolean isJsonNode() {
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
     * Retrieves the parent LogicNode. Not applicable for JsonNode.
     * @throws UnsupportedOperationException always, as JsonNode does not have a parent LogicNode.
     */
    @Override
    public LogicNode getParentNode() {
        // Corrected exception message to reflect JsonNode (or a more generic message)
        throw new UnsupportedOperationException("JsonNode does not have a parent LogicNode.");
    }
}