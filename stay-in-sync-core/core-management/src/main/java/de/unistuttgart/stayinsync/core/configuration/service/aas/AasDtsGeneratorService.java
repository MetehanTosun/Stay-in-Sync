package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSubmodelLite;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class AasDtsGeneratorService {
    public String generateDtsForSubmodel(AasSubmodelLite submodel) {
        List<AasElementLite> elements = AasElementLite.list("submodelLite.id", submodel.id);
        if (elements.isEmpty()) {
            return "interface Root {}";
        }

        Map<String, AasElementLite> elementMap = elements.stream()
                .collect(Collectors.toMap(e -> e.idShortPath, e -> e));

        Map<String, List<AasElementLite>> hierarchy = elements.stream()
                .filter(e -> e.parentPath != null)
                .collect(Collectors.groupingBy(e -> e.parentPath));

        Set<String> generatedInterfaces = new HashSet<>();
        StringBuilder allInterfaces = new StringBuilder();

        List<AasElementLite> rootElements = elements.stream()
                .filter(e -> e.parentPath == null || e.parentPath.isEmpty())
                .toList();

        allInterfaces.append(generateInterface("Root", rootElements, hierarchy, elementMap, generatedInterfaces));

        return allInterfaces.toString();
    }

    private String generateInterface(String interfaceName, List<AasElementLite> children, Map<String, List<AasElementLite>> hierarchy, Map<String, AasElementLite> elementMap, Set<String> generatedInterfaces) {
        if (generatedInterfaces.contains(interfaceName)) {
            return "";
        }
        generatedInterfaces.add(interfaceName);

        StringBuilder sb = new StringBuilder();
        StringBuilder nestedInterfaces = new StringBuilder();

        sb.append(String.format("interface %s {\n", interfaceName));

        for (AasElementLite child : children) {
            String fieldName = sanitizeForTs(child.idShort);
            String fieldType;

            if (child.hasChildren) {
                fieldType = capitalize(fieldName) + "Type";
                List<AasElementLite> grandChildren = hierarchy.getOrDefault(child.idShortPath, List.of());
                nestedInterfaces.append(generateInterface(fieldType, grandChildren, hierarchy, elementMap, generatedInterfaces));
            } else {
                fieldType = mapValueTypeToTs(child.valueType);
            }
            sb.append(String.format("  %s: %s;\n", fieldName, fieldType));
        }
        sb.append("}\n");

        nestedInterfaces.append(sb);
        return nestedInterfaces.toString();
    }

    private String mapValueTypeToTs(String valueType) {
        if (valueType == null) return "any";
        return switch (valueType.toLowerCase()) {
            case "xs:string", "string" -> "string";
            case "xs:double", "xs:int", "xs:integer", "xs:long", "number" -> "number";
            case "xs:boolean", "boolean" -> "boolean";
            default -> "any";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Sanitizes a string to make it a valid TypeScript identifier.
     * It performs the following operations:
     * 1. Replaces spaces, dashes, and other common separators with an underscore.
     * 2. Removes any characters that are not alphanumeric or an underscore.
     * 3. Ensures the identifier does not start with a number by prepending an underscore.
     *
     * @param s The string to sanitize.
     * @return A valid TypeScript identifier.
     */
    private String sanitizeForTs(String s) {
        if (s == null || s.isEmpty()) {
            return "_invalidIdentifier";
        }

        String sanitized = s.replaceAll("[\\s\\-./]", "_");

        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_]", "");

        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }

        if (sanitized.isEmpty()) {
            return "_invalidIdentifier";
        }

        return sanitized;
    }
}
