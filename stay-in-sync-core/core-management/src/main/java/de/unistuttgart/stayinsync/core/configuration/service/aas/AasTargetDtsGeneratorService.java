package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.TypeLibraryDTO;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates TypeScript Definition files (.d.ts) for AAS (Asset Administration Shell) Target ARCs.
 * <p>
 * This service is responsible for translating the hierarchical structure of an AAS Submodel,
 * which is persisted as a flattened list of {@link AasElementLite} entities, into a deeply nested,
 * fully typed client API for use in the script editor. It recursively builds a TypeScript class
 * structure that mirrors the Submodel's elements, providing script-writers with a fluent,
 * type-safe way to define directives for interacting with AAS Submodels.
 */
@ApplicationScoped
public class AasTargetDtsGeneratorService {

    /**
     * The main public entry point for the service. It orchestrates the generation of TypeScript
     * definition libraries for a given set of AAS Target ARCs. It iterates through each ARC,
     * generating a dedicated, self-contained .d.ts file for it.
     *
     * @param aasArcs A set of {@link AasTargetApiRequestConfiguration} entities for which to generate types.
     * @return A {@code List} of {@link TypeLibraryDTO} objects, each representing a generated .d.ts file.
     * Returns an empty list if the input set is null or empty.
     */
    public List<TypeLibraryDTO> generateForAasArcs(Set<AasTargetApiRequestConfiguration> aasArcs) {
        if (aasArcs == null || aasArcs.isEmpty()) {
            return Collections.emptyList();
        }

        List<TypeLibraryDTO> libraries = new ArrayList<>();
        for (AasTargetApiRequestConfiguration arc : aasArcs) {
            libraries.add(generateArcLibrary(arc));
        }
        return libraries;
    }

    /**
     * Generates the complete .d.ts file content for a single AAS Target ARC.
     * This method fetches the structural snapshot of the associated Submodel (as a list of {@link AasElementLite}),
     * reconstructs its hierarchy, and then recursively generates the TypeScript class and interface definitions.
     *
     * @param arc The {@link AasTargetApiRequestConfiguration} to process.
     * @return A {@link TypeLibraryDTO} containing the file path and the generated TypeScript content.
     */
    private TypeLibraryDTO generateArcLibrary(AasTargetApiRequestConfiguration arc) {
        List<AasElementLite> elements = AasElementLite.list("submodelLite.id", arc.submodel.id);
        if (elements.isEmpty()) {
            return createEmptyArcLibrary(arc);
        }

        Map<String, List<AasElementLite>> hierarchy = elements.stream()
                .filter(e -> e.parentPath != null && !e.parentPath.isEmpty())
                .collect(Collectors.groupingBy(AasElementLite::getParentPath));

        List<AasElementLite> rootElements = elements.stream()
                .filter(e -> e.parentPath == null || e.parentPath.isEmpty())
                .toList();

        StringBuilder dts = new StringBuilder();
        String clientClassName = toPascalCase(arc.alias) + "_Client";
        String directiveBaseName = toPascalCase(arc.alias);
        Set<String> generatedNestedTypes = new HashSet<>();

        dts.append(generateClientClass(clientClassName, rootElements, hierarchy, generatedNestedTypes));

        dts.append(String.format("declare interface %s_SetValueDirective extends TargetDirective {}\n", directiveBaseName));
        dts.append(String.format("declare interface %s_AddElementDirective extends TargetDirective {}\n", directiveBaseName));
        dts.append(String.format("declare interface %s_DeleteElementDirective extends TargetDirective {}\n", directiveBaseName));

        String filePath = "stayinsync/targets/aas/" + arc.alias + ".d.ts";
        return new TypeLibraryDTO(filePath, dts.toString());
    }

    /**
     * Recursively generates the TypeScript {@code declare class} structure for a given level of the Submodel hierarchy,
     * along with any nested {@code interface} types required by its children.
     *
     * @param className            The name of the TypeScript class to generate.
     * @param children             A list of {@link AasElementLite} representing the direct children at this level of the hierarchy.
     * @param hierarchy            A map used for efficiently looking up the children of any element by its path.
     * @param generatedNestedTypes A set to track the names of already generated nested types to prevent duplication.
     * @return A {@code String} containing the complete TypeScript definition for the class and its nested types.
     */
    private String generateClientClass(String className, List<AasElementLite> children, Map<String, List<AasElementLite>> hierarchy, Set<String> generatedNestedTypes) {
        StringBuilder classContent = new StringBuilder();
        StringBuilder nestedInterfaces = new StringBuilder();

        classContent.append(String.format("declare class %s {\n", className));

        for (AasElementLite child : children) {
            String fieldName = sanitizeForTs(child.idShort);
            String fieldType = generateFieldType(child, hierarchy, nestedInterfaces, generatedNestedTypes);
            classContent.append(String.format("  readonly %s: %s;\n", fieldName, fieldType));
        }

        classContent.append("}\n");

        nestedInterfaces.append(classContent);
        return nestedInterfaces.toString();
    }

    /**
     * Generates the TypeScript type for a single field within a class or an interface. This is the central
     * recursive method that determines the shape of the generated API for each element.
     * <ul>
     *   <li>For a "Property" element, it generates an object with a {@code setValue} method.</li>
     *   <li>For a "SubmodelElementList" or "SubmodelElementCollection", it generates a new, complex interface
     *       type with methods like {@code addElement} and {@code deleteElement}, and then recursively calls itself
     *       to define the properties for the collection's children.</li>
     * </ul>
     *
     * @param element              The {@link AasElementLite} to generate a type for.
     * @param hierarchy            The complete hierarchy map for child lookups.
     * @param nestedInterfaces     A {@link StringBuilder} to which any newly generated nested interface definitions are appended.
     * @param generatedNestedTypes A set for tracking already generated types to avoid duplicates.
     * @return A {@code String} representing the TypeScript type for the element's field (e.g., "{ setValue... }", "MyCollectionType", "any").
     */
    private String generateFieldType(AasElementLite element, Map<String, List<AasElementLite>> hierarchy, StringBuilder nestedInterfaces, Set<String> generatedNestedTypes) {
        String baseName = toPascalCase(element.idShort);
        String directiveBaseName = toPascalCase(element.submodelLite.sourceSystem.name) + "_" + toPascalCase(element.submodelLite.submodelIdShort);

        if ("Property".equals(element.modelType)) {
            String valueType = mapValueTypeToTs(element.valueType);
            return String.format("{ setValue(newValue: %s): { build(): %s_SetValueDirective } }", valueType, directiveBaseName);
        }

        if ("SubmodelElementList".equals(element.modelType) || "SubmodelElementCollection".equals(element.modelType)) {
            String collectionTypeName = baseName + "Type";

            if (!generatedNestedTypes.contains(collectionTypeName)) {
                generatedNestedTypes.add(collectionTypeName);

                String elementType = "any";

                StringBuilder collectionInterface = new StringBuilder();
                collectionInterface.append(String.format("interface %s {\n", collectionTypeName));
                collectionInterface.append(String.format("  addElement(newElement: %s): { build(): %s_AddElementDirective };\n", elementType, directiveBaseName));
                collectionInterface.append(String.format("  deleteElement(idShort: string): { build(): %s_DeleteElementDirective };\n", directiveBaseName));

                List<AasElementLite> children = hierarchy.getOrDefault(element.idShortPath, Collections.emptyList());
                for (AasElementLite child : children) {
                    String fieldName = sanitizeForTs(child.idShort);
                    String fieldType = generateFieldType(child, hierarchy, nestedInterfaces, generatedNestedTypes);
                    collectionInterface.append(String.format("  readonly %s: %s;\n", fieldName, fieldType));
                }
                collectionInterface.append("}\n");
                nestedInterfaces.append(collectionInterface);
            }

            return collectionTypeName;
        }

        return "any";
    }

    /**
     * A fallback method that creates a placeholder type definition for an AAS ARC. This is used when the
     * ARC's submodel has no structural snapshot data (i.e., no {@link AasElementLite} entities are found),
     * preventing errors in the script editor.
     *
     * @param arc The {@link AasTargetApiRequestConfiguration} for which to create the empty library.
     * @return A {@link TypeLibraryDTO} containing a simple, empty class declaration.
     */
    private TypeLibraryDTO createEmptyArcLibrary(AasTargetApiRequestConfiguration arc) {
        String clientClassName = toPascalCase(arc.alias) + "_Client";
        String content = String.format("declare class %s {}\n", clientClassName);
        String filePath = "stayinsync/targets/aas/" + arc.alias + ".d.ts";
        return new TypeLibraryDTO(filePath, content);
    }

    /**
     * A utility method to map a value type string (often from an XSD schema) to a primitive TypeScript type.
     *
     * @param valueType The source value type string (e.g., "xs:string", "boolean").
     * @return The corresponding TypeScript type as a {@code String} (e.g., "string", "boolean", "any").
     */
    private String mapValueTypeToTs(String valueType) {
        if (valueType == null || valueType.isBlank()) return "any";
        return switch (valueType.toLowerCase()) {
            case "xs:string", "string" -> "string";
            case "xs:double", "xs:int", "xs:integer", "xs:long", "xs:float", "xs:decimal", "number" -> "number";
            case "xs:boolean", "boolean" -> "boolean";
            default -> "any";
        };
    }

    /**
     * A simple utility method to capitalize the first letter of a string.
     *
     * @param s The string to capitalize.
     * @return The capitalized string.
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * A utility function to convert a string from any case (e.g., snake_case, kebab-case) to PascalCase,
     * suitable for TypeScript class or interface names.
     *
     * @param s The input string.
     * @return The PascalCase version of the string.
     */
    private String toPascalCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return Arrays.stream(s.split("[_\\- ]"))
                .map(this::capitalize)
                .collect(Collectors.joining());
    }

    /**
     * A utility function that sanitizes a string to ensure it is a valid TypeScript/JavaScript identifier.
     * It replaces invalid characters with underscores and prepends an underscore if the string starts with a digit.
     * This is crucial because AAS {@code idShort} values can contain characters that are not permitted in variable names.
     *
     * @param s The input string (typically an {@code idShort}) to sanitize.
     * @return A valid TypeScript identifier as a {@code String}.
     */
    private String sanitizeForTs(String s) {
        if (s == null || s.isEmpty()) {
            return "_invalidIdentifier";
        }
        String sanitized = s.replaceAll("[\\s\\-./]", "_");
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_]", "");
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }
        return sanitized;
    }
}