package de.unistuttgart.stayinsync.core.configuration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Iterator;
import java.util.Map;

@ApplicationScoped
public class TypeScriptTypeGenerator {

    private final ObjectMapper mapper = new ObjectMapper();

    public String generate(String jsonString) throws JsonProcessingException {
        JsonNode rootNode = mapper.readTree(jsonString);
        StringBuilder allInterfaces = new StringBuilder();

        generateInterface("Root", rootNode, allInterfaces);
        return allInterfaces.toString();
    }

    private void generateInterface(String interfaceName, JsonNode node, StringBuilder allInterfaces) {
        StringBuilder currentInterface = new StringBuilder();
        currentInterface.append(String.format("interface %s {\n", interfaceName));

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldNode = field.getValue();
            String type = getTsType(fieldName, fieldNode, allInterfaces);
            currentInterface.append(String.format(" %s: %s;\n", fieldName, type));
        }
        currentInterface.append("}\n");
        // Prepending for proper order
        allInterfaces.insert(0, currentInterface);
    }

    private String getTsType(String fieldName, JsonNode fieldNode, StringBuilder allInterfaces) {
        if (fieldNode.isTextual()) return "string";
        if (fieldNode.isNumber()) return "number";
        if (fieldNode.isBoolean()) return "boolean";
        if (fieldNode.isNull()) return "any";

        if (fieldNode.isObject()){
           String nestedInterfaceName = capitalize(fieldName) + "Type";
           generateInterface(nestedInterfaceName, fieldNode, allInterfaces);
           return nestedInterfaceName;
        }

        if (fieldNode.isArray()){
            if (fieldNode.isEmpty()){
                return "any[]";
            }
            JsonNode firstElement = fieldNode.get(0);
            String arrayElementType = getTsType(fieldName, firstElement, allInterfaces);
            return arrayElementType + "[]";
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
