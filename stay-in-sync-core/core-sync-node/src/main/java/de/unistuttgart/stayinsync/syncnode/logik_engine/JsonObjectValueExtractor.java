package de.unistuttgart.stayinsync.syncnode.logik_engine;

import jakarta.json.JsonObject;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.util.Optional;

public class JsonObjectValueExtractor {

    /**
     * Extracts a single value from the root {@link JsonObject} based on the provided path.
     * The path is a dot-separated string of keys to navigate through nested JSON objects
     *
     * @param rootJsonObject The root JSON object from which to extract the value.
     * @param pathToExtract  The dot-separated path string to the desired value.
     * @return An {@link Optional} containing the extracted Java object if the path is valid
     *         and the value at the path is not JSON null. Returns {@link Optional#empty()}
     *         if the path is not found, if any intermediate path segment is not an object,
     *         if {@code rootJsonObject} is null, if {@code pathToExtract} is null or empty,
     *         or if the value at the path is JSON null.
     */
    public Optional<Object> extractValue(JsonObject rootJsonObject, String pathToExtract) {
        if (rootJsonObject == null || pathToExtract == null || pathToExtract.trim().isEmpty()) {
            return Optional.empty();
        }

        String[] segments = pathToExtract.split("\\.");
        JsonValue currentValue = rootJsonObject;

        for (String segment : segments) {
            segment = segment.trim();
            if (segment.isEmpty()) {
                return Optional.empty();
            }

            if (currentValue == null || currentValue.getValueType() != JsonValue.ValueType.OBJECT) {
                return Optional.empty();
            }

            JsonObject currentObject = (JsonObject) currentValue;
            if (currentObject.containsKey(segment)) {
                currentValue = currentObject.get(segment);
            } else {
                return Optional.empty();
            }
        }

        if (currentValue != null && currentValue.getValueType() != JsonValue.ValueType.NULL) {
            return Optional.ofNullable(convertJsonValueToJavaObject(currentValue));
        } else {
            return Optional.empty();
        }
    }


    /**
     * Converts a {@link JsonValue} to a corresponding Java primitive wrapper or String.
     * Handles JSON string, number (long or double), true, and false.
     *
     * @param jsonValue The {@link JsonValue} to convert.
     * @return The converted Java object.
     * @throws IllegalArgumentException if the {@code jsonValue.getValueType()} is not one of
     *                                  STRING, NUMBER, TRUE, or FALSE.
     */
    private Object convertJsonValueToJavaObject(JsonValue jsonValue) {
        switch (jsonValue.getValueType()) {
            case STRING:
                return ((JsonString) jsonValue).getString();
            case NUMBER:
                JsonNumber jsonNumber = (JsonNumber) jsonValue;
                if (jsonNumber.isIntegral()) {
                    return jsonNumber.longValue();
                } else {
                    return jsonNumber.doubleValue();
                }
            case TRUE:
                return true;
            case FALSE:
                return false;
            default:
                throw new IllegalArgumentException("Unhandled JsonValue type: " + jsonValue.getValueType());
        }
    }
}
