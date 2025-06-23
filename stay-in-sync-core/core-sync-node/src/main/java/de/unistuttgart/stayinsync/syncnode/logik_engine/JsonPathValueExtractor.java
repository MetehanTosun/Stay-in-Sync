package de.unistuttgart.stayinsync.syncnode.logik_engine;

import com.fasterxml.jackson.databind.JsonNode; // Jackson-Import
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.util.Optional;

public class JsonPathValueExtractor {

    /**
     * Extracts a single value from the root Jackson JsonNode based on the provided path.
     * Uses Jackson's safe path traversal.
     *
     * @param rootNode The root JsonNode from which to extract the value.
     * @param pathToExtract The dot-separated path string (e.g., "a.b.c").
     * @return An Optional containing the extracted Java object if the path is valid and the
     *         value is not missing or explicitly JSON null.
     */
    public Optional<Object> extractValue(JsonNode rootNode, String pathToExtract) {
        if (rootNode == null || rootNode.isMissingNode() || pathToExtract == null || pathToExtract.trim().isEmpty()) {
            return Optional.empty();
        }

        JsonNode targetNode = navigateTo(rootNode, pathToExtract);

        if (targetNode.isMissingNode() || targetNode.isNull()) {
            return Optional.empty();
        }

        return Optional.of(convertJsonNodeToJavaObject(targetNode));
    }

    /**
     * Checks if a given path exists within a JsonNode. A path exists if it can be
     * fully navigated, even if the resulting value is explicitly null.
     *
     * @param rootNode The root JsonNode to search within.
     * @param pathToExtract  The dot-separated path string.
     * @return {@code true} if the path exists, {@code false} otherwise.
     */
    public boolean pathExists(JsonNode rootNode, String pathToExtract) {
        if (rootNode == null || rootNode.isMissingNode() || pathToExtract == null || pathToExtract.trim().isEmpty()) {
            return false;
        }

        JsonNode targetNode = navigateTo(rootNode, pathToExtract);

        // The path "exists" if the final node is anything other than a "MissingNode".
        // A node with a 'null' value is considered to exist.
        return !targetNode.isMissingNode();
    }

    /**
     * Navigates through a JsonNode using a dot-separated path string.
     * @return The target JsonNode, or a MissingNode if the path is invalid.
     */
    private JsonNode navigateTo(JsonNode rootNode, String path) {
        JsonNode currentNode = rootNode;
        String[] segments = path.split("\\.");
        for (String segment : segments) {
            currentNode = currentNode.path(segment);
        }
        return currentNode;
    }

    /**
     * Converts a Jackson JsonNode to a corresponding Java primitive wrapper or String.
     */
    private Object convertJsonNodeToJavaObject(JsonNode jsonNode) {
        JsonNodeType nodeType = jsonNode.getNodeType();
        switch (nodeType) {
            case STRING:  return jsonNode.asText();
            case NUMBER:  return jsonNode.isIntegralNumber() ? jsonNode.asLong() : jsonNode.asDouble();
            case BOOLEAN: return jsonNode.asBoolean();
            // Note: ARRAY and OBJECT types could be converted to List/Map here if needed.
            default:
                throw new IllegalArgumentException("Unhandled JsonNode type for conversion: " + nodeType);
        }
    }
}