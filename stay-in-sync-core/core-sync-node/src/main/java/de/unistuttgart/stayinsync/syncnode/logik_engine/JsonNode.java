package de.unistuttgart.stayinsync.syncnode.logik_engine;

import jakarta.json.JsonObject;
import java.util.Optional;

/**
 * Represents an input node that sources its value from a specific path
 * within an external JSON object (typically representing AAS data).
 * The source JsonObject and path are defined at creation.
 */
public class JsonNode implements InputNode {
    private final String jsonPath;
    private final JsonObject sourceJsonObject;

    /**
     * Constructs a new JsonNode.
     *
     * @param source The JsonObject acting as the data source. Cannot be null.
     * @param path   The dot-separated path to the value within the source JsonObject.
     *               Cannot be null or empty.
     * @throws IllegalArgumentException if source is null, or if path is null or empty.
     */
    public JsonNode(JsonObject source, String path) {
        if (source == null) {
            throw new IllegalArgumentException("Source JsonObject for JsonNode (for path: '" + path + "') cannot be null.");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON path for JsonNode cannot be null or empty.");
        }
        this.sourceJsonObject = source;
        this.jsonPath = path;
    }

    /**
     * Retrieves the value from the source JsonObject using the configured jsonPath.
     *
     * @return The extracted value as an Object.
     * @throws IllegalStateException if the path does not resolve to a value or if the value is JSON null.
     */
    @Override
    public Object getValue() {
        // JsonObjectValueExtractor is used internally to perform the extraction.
        JsonObjectValueExtractor extractor = new JsonObjectValueExtractor();
        Optional<Object> extractedOptional = extractor.extractValue(this.sourceJsonObject, this.jsonPath);

        // If the Optional is empty (path not found or value was JSON null), throw an exception.
        return extractedOptional.orElseThrow(() ->
                new IllegalStateException("Value for path '" + jsonPath + "' not found or was null in the provided JsonObject.")
        );
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