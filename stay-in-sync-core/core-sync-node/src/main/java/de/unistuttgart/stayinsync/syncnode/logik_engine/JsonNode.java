package de.unistuttgart.stayinsync.syncnode.logik_engine;

import jakarta.json.JsonObject;

import java.util.Optional;

/**
 * Represents an input provider that sources its value from a specific path
 * within an external JSON object (typically representing AAS data).
 */
public class JsonNode implements InputNode {
    private final String jsonPath;
    private final JsonObject sourceJsonObject;


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

    @Override
    public Object getValue() {
        JsonObjectValueExtractor extractor = new JsonObjectValueExtractor();
        Optional<Object> extractedOptional = extractor.extractValue(this.sourceJsonObject, this.jsonPath);

        return extractedOptional.orElseThrow(() ->
                new IllegalStateException("Value for path '" + jsonPath + "' not found or was null in the provided JsonObject.")
        );
    }

    /**
     * @return false, as this provider is not sourced from another LogicNode.
     */
    @Override
    public boolean isParentNode() {
        return false;
    }

    /**
     * @return true, as this provider is sourced from an external JSON path.
     */
    @Override
    public boolean isJsonNode() {
        return true;
    }

    /**
     * @return false, as this provider is not sourced from a UI element.
     */
    @Override
    public boolean isConstantNode() {
        return false;
    }

    /**
     * This operation is not supported for ExternalInput.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public LogicNode getParentNode() {
        throw new UnsupportedOperationException("ExternalInput does not provide a parent node.");
    }

}