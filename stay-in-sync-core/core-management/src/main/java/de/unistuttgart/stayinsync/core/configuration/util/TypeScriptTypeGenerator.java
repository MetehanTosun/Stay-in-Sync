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
        StringBuilder allInterfaces = new StringBuilder();
        Set<String> generatedInterfaceNames = new HashSet<>();

        if(rootNode.isArray()){
            if (!rootNode.isEmpty()){
                generateInterface("RootObject", rootNode.get(0), allInterfaces, generatedInterfaceNames);
            }
        } else if(rootNode.isObject()){
            generateInterface("Root", rootNode, allInterfaces, generatedInterfaceNames);
        }

        return allInterfaces.toString();
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
