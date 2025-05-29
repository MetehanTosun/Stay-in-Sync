package de.unistuttgart.stayinsync.syncnode.logik_engine;


import jakarta.json.JsonObject;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.Optional;

public class JsonObjectValueExtractor {

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
