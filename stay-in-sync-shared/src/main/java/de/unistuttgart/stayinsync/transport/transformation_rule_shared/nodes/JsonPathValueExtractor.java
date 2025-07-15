package de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes;

import com.fasterxml.jackson.databind.JsonNode; // Jackson-Import
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.util.Optional;

public class JsonPathValueExtractor {

    /**
     * Extracts a single value from the root Jackson JsonNode based on the provided path.
     * <p>
     * Special path conventions:
     * <ul>
     *     <li>{@code "/"}, {@code "$"}, or an empty string: Returns the root node itself.</li>
     *     <li>Dot-separated path (e.g., "a.b.c"): Navigates through the JSON structure.</li>
     * </ul>
     *
     * @param rootNode The root JsonNode from which to extract the value.
     * @param pathToExtract The path string.
     * @return An Optional containing the extracted Java object or the root JsonNode itself.
     *         Returns an empty Optional if the path is invalid or the value is missing/null.
     */
    public Optional<Object> extractValue(JsonNode rootNode, String pathToExtract) {
        if (rootNode == null || rootNode.isMissingNode() || pathToExtract == null) {
            return Optional.empty();
        }

        // Handle a path that refers to the root object itself.
        String trimmedPath = pathToExtract.trim();
        if (trimmedPath.equals("/") || trimmedPath.equals("$") || trimmedPath.isEmpty()) {
            // Check if the root itself is null before converting
            if (rootNode.isNull()) {
                return Optional.empty();
            }
            // If the root is an object or array, return the JsonNode itself.
            // If it's a primitive value, convert it.
            if (rootNode.isContainerNode()) { // isContainerNode() covers OBJECT and ARRAY
                return Optional.of(rootNode);
            }
            // For root-level primitive values, convert them like any other value
            return Optional.of(convertJsonNodeToJavaObject(rootNode));
        }

        JsonNode targetNode = navigateTo(rootNode, trimmedPath);

        if (targetNode.isMissingNode() || targetNode.isNull()) {
            return Optional.empty();
        }

        return Optional.of(convertJsonNodeToJavaObject(targetNode));
    }

    /**
     * Checks if a given path exists within a JsonNode. A path exists if it can be
     * fully navigated, even if the resulting value is explicitly null.
     * Also handles root-path conventions.
     *
     * @param rootNode The root JsonNode to search within.
     * @param pathToExtract  The dot-separated path string.
     * @return {@code true} if the path exists, {@code false} otherwise.
     */
    public boolean pathExists(JsonNode rootNode, String pathToExtract) {
        if (rootNode == null || rootNode.isMissingNode() || pathToExtract == null) {
            return false;
        }

        // Handle root-path conventions for existence check.
        String trimmedPath = pathToExtract.trim();
        if (trimmedPath.equals("/") || trimmedPath.equals("$") || trimmedPath.isEmpty()) {
            // The path to the root exists as long as the root is not a "MissingNode".
            // A root that is 'null' still exists.
            return !rootNode.isMissingNode();
        }

        JsonNode targetNode = navigateTo(rootNode, trimmedPath);
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
            case JsonNodeType.STRING:  return jsonNode.asText();
            case JsonNodeType.NUMBER:  return jsonNode.isIntegralNumber() ? jsonNode.asLong() : jsonNode.asDouble();
            case JsonNodeType.BOOLEAN: return jsonNode.asBoolean();
            // Note: ARRAY and OBJECT types could be converted to List/Map here if needed.
            default:
                throw new IllegalArgumentException("Unhandled JsonNode type for conversion: " + nodeType);
        }
    }
}