package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSubmodelLite;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This service is responsible for generating a TypeScript Definition File (.d.ts) that represents
 * the static data structure of an AAS (Asset Administration Shell) Submodel. It is designed to provide
 * type information for AAS-based **Source ARCs** in the script editor.
 * <p>
 * The service operates on a simplified, flattened database representation of the Submodel (a list of
 * {@link AasElementLite} entities). It reconstructs the original parent-child hierarchy from this list
 * and recursively generates a series of nested TypeScript {@code interface} definitions, starting with a
 * top-level {@code Root} interface. This enables strong typing and autocompletion for the data payloads
 * retrieved from AAS sources within the user's script.
 */
@ApplicationScoped
public class AasDtsGeneratorService {

    /**
     * The main public entry point for the service. It orchestrates the generation of a complete .d.ts
     * string for a given Submodel. It fetches the structural elements of the submodel, prepares the
     * necessary data structures for hierarchical traversal (maps and lists), and then initiates the
     * recursive interface generation process starting from the "Root" level.
     *
     * @param submodel The {@link AasSubmodelLite} entity that defines which submodel structure to generate types for.
     * @return A {@code String} containing the complete .d.ts content for the entire submodel structure.
     * Returns a default empty "Root" interface if the submodel has no elements.
     */
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

    /**
     * The core recursive engine of the generator. It generates a single TypeScript {@code interface} block for a
     * given name and its list of child elements.
     * <p>
     * For each child element, it determines if the child itself has nested children. If so, it recursively calls
     * this method to generate a new, nested interface (e.g., {@code EngineDetailsType}) and types the parent's
     * field with that new interface name. If the child is a primitive property, its type is mapped directly.
     * This process builds up a complete, nested type structure.
     *
     * @param interfaceName       The name for the TypeScript {@code interface} being generated (e.g., "Root", "EngineDetailsType").
     * @param children            The list of {@link AasElementLite} that are direct properties of this interface.
     * @param hierarchy           The complete map of the submodel structure, used for efficiently looking up the children of any element.
     * @param elementMap          A map for direct lookup of elements by their unique path (not actively used in recursion but available).
     * @param generatedInterfaces A state-tracking set to prevent infinite recursion and duplicate interface generation by keeping a record of which interface names have already been processed.
     * @return A {@code String} containing the generated {@code interface} block for the current level, along with any nested interfaces generated during the process.
     */
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

    /**
     * A utility method to map a data type string (often from an XSD schema like "xs:integer") to its
     * corresponding primitive TypeScript type.
     *
     * @param valueType The input data type string.
     * @return The corresponding TypeScript type as a {@code String} (e.g., "string", "number", "boolean", "any").
     */
    private String mapValueTypeToTs(String valueType) {
        if (valueType == null) return "any";
        return switch (valueType.toLowerCase()) {
            case "xs:string", "string" -> "string";
            case "xs:double", "xs:int", "xs:integer", "xs:long", "number" -> "number";
            case "xs:boolean", "boolean" -> "boolean";
            default -> "any";
        };
    }

    /**
     * A simple utility method to capitalize the first letter of a given string.
     *
     * @param s The string to capitalize.
     * @return The capitalized string, or the original string if it is null or empty.
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Sanitizes a string to make it a valid TypeScript identifier, which is crucial for property names inside interfaces.
     * It performs the following operations:
     * <ol>
     *   <li>Replaces spaces, dashes, slashes, and other common separators with an underscore.</li>
     *   <li>Removes any characters that are not alphanumeric or an underscore.</li>
     *   <li>Ensures the identifier does not start with a number by prepending an underscore if necessary.</li>
     * </ol>
     *
     * @param s The string to sanitize, typically an AAS {@code idShort}.
     * @return A {@code String} that is a valid TypeScript identifier.
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
