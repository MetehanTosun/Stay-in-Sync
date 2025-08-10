package de.unistuttgart.stayinsync.core.configuration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class TypeScriptTypeGenerator {

    private final ObjectMapper mapper = new ObjectMapper();

    public String generate(String jsonString) throws JsonProcessingException {
        JsonNode rootNode = mapper.readTree(jsonString);
        // Heuristics: If node looks like a JSON Schema (has "properties" or "$schema" or "type")
        // prefer schema-based generation. Otherwise, treat as example JSON.
        if (looksLikeJsonSchema(rootNode)) {
            return generateFromJsonSchema(rootNode);
        }

        StringBuilder allInterfaces = new StringBuilder();
        Set<String> generatedInterfaceNames = new HashSet<>();

        if (rootNode.isArray()) {
            if (!rootNode.isEmpty()) {
                generateInterface("RootObject", rootNode.get(0), allInterfaces, generatedInterfaceNames);
            }
        } else if (rootNode.isObject()) {
            generateInterface("Root", rootNode, allInterfaces, generatedInterfaceNames);
        }

        return allInterfaces.toString();
    }

    private boolean looksLikeJsonSchema(JsonNode node) {
        if (node == null || !node.isObject()) return false;
        return node.has("properties") || node.has("$schema") || node.has("type") || node.has("items")
            || node.has("allOf") || node.has("anyOf") || node.has("oneOf");
    }

    private String generateFromJsonSchema(JsonNode schema) {
        StringBuilder builder = new StringBuilder();
        if (isArraySchema(schema)) {
            String ts = schemaTypeToTs(schema.get("items"));
            builder.append("export type ResponseBody = ").append(ts).append("[];\n");
            return builder.toString();
        }

        if (isObjectSchema(schema)) {
            builder.append("export interface ResponseBody {\n");
            JsonNode props = schema.get("properties");
            Set<String> required = readRequired(schema);
            if (props != null && props.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String name = field.getKey();
                    JsonNode child = field.getValue();
                    String tsType = schemaTypeToTs(child);
                    boolean isReq = required.contains(name);
                    String validFieldName = name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$") ? name : String.format("'%s'", name);
                    builder.append("  ").append(validFieldName).append(isReq ? ": " : "?: ").append(tsType).append(";\n");
                }
            }
            builder.append("}\n");
            return builder.toString();
        }

        // Fallback for primitive schema
        builder.append("export type ResponseBody = ").append(schemaTypeToTs(schema)).append(";\n");
        return builder.toString();
    }

    private boolean isObjectSchema(JsonNode schema) {
        return schema != null && schema.isObject() && "object".equals(textOrNull(schema.get("type")));
    }

    private boolean isArraySchema(JsonNode schema) {
        return schema != null && schema.isObject() && "array".equals(textOrNull(schema.get("type")));
    }

    private Set<String> readRequired(JsonNode schema) {
        Set<String> req = new HashSet<>();
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode r : required) if (r.isTextual()) req.add(r.asText());
        }
        return req;
    }

    private String schemaTypeToTs(JsonNode node) {
        if (node == null || node.isNull()) return "any";

        // Handle $ref loosely (could be resolved beforehand)
        if (node.has("$ref")) return "any";

        // Enum support
        if (node.has("enum") && node.get("enum").isArray()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (JsonNode v : node.get("enum")) {
                if (!first) sb.append(" | ");
                if (v.isNumber()) sb.append(v.asText()); else sb.append("'" + v.asText() + "'");
                first = false;
            }
            return sb.toString();
        }

        String type = textOrNull(node.get("type"));
        if ("object".equals(type)) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            JsonNode props = node.get("properties");
            Set<String> required = readRequired(node);
            if (props != null && props.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = props.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    String key = e.getKey();
                    String valid = key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$") ? key : String.format("'%s'", key);
                    boolean isReq = required.contains(key);
                    sb.append("  ").append(valid).append(isReq ? ": " : "?: ").append(schemaTypeToTs(e.getValue())).append(";\n");
                }
            }
            // additionalProperties
            if (node.has("additionalProperties")) {
                JsonNode addl = node.get("additionalProperties");
                String vType = addl.isBoolean() ? (addl.asBoolean() ? "any" : "never") : schemaTypeToTs(addl);
                sb.append("  [key: string]: ").append(vType).append(";\n");
            }
            sb.append("}\n");
            return sb.toString();
        }
        if ("array".equals(type)) {
            return schemaTypeToTs(node.get("items")) + "[]";
        }
        if ("string".equals(type)) return "string";
        if ("integer".equals(type) || "number".equals(type)) return "number";
        if ("boolean".equals(type)) return "boolean";
        if ("null".equals(type)) return "null";
        return "any";
    }

    private String textOrNull(JsonNode n) {
        return n != null && n.isTextual() ? n.asText() : null;
    }

    private void generateInterface(String interfaceName, JsonNode node, StringBuilder allInterfaces, Set<String> generatedNames) {
        if(generatedNames.contains(interfaceName)){
            return;
        }

        StringBuilder currentInterface = new StringBuilder();
        currentInterface.append(String.format("interface %s {\n", interfaceName));

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            String validFieldName = fieldName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$") ? fieldName : String.format("'%s'", fieldName);
            JsonNode fieldNode = field.getValue();
            String type = getTsType(fieldName, fieldNode, allInterfaces, generatedNames);
            currentInterface.append(String.format(" %s: %s;\n", validFieldName, type));
        }
        currentInterface.append("}\n");
        // Prepending ensures that dependencies are defined before they are used.
        allInterfaces.insert(0, currentInterface);
        generatedNames.add(interfaceName);
    }

    private String getTsType(String fieldName, JsonNode fieldNode, StringBuilder allInterfaces, Set<String> generatedNames) {
        if (fieldNode.isTextual()) return "string";
        if (fieldNode.isNumber()) return "number";
        if (fieldNode.isBoolean()) return "boolean";
        if (fieldNode.isNull()) return "any";

        if (fieldNode.isObject()){
           String nestedInterfaceName = capitalize(fieldName) + "Type";
           generateInterface(nestedInterfaceName, fieldNode, allInterfaces, generatedNames);
           return nestedInterfaceName;
        }

        if (fieldNode.isArray()){
            if (fieldNode.isEmpty()){
                return "any[]";
            }
            JsonNode firstElement = fieldNode.get(0);
            String singularFieldName = fieldName.endsWith("s") ? fieldName.substring(0, fieldName.length() - 1) : fieldName;
            String arrayElementType = getTsType(singularFieldName, firstElement, allInterfaces, generatedNames);
            return String.format("%s[]", arrayElementType);
        }

        return "any";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
