package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.TypeLibraryDTO;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates TypeScript Definition files (.d.ts) for AAS Target ARCs.
 * This service reads the structure of a Submodel from the AasElementLite snapshot
 * and creates a deeply nested, fully typed client API for the script editor.
 */
@ApplicationScoped
public class AasTargetDtsGeneratorService {

    /**
     * Main entry point. Generates a list of TypeLibraryDTOs for a set of AAS ARCs.
     * Each ARC gets its own type definition file.
     * @param aasArcs A set of AAS Target ARCs to generate types for.
     * @return A list of TypeLibraryDTOs ready to be sent to the frontend.
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
     * Recursively generates the `declare class` structure and all nested `interface` types.
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
     * Generates the typescript type for a single field inside a class or an interface.
     * This is the central method, which decides if a field will be a simple `setValue` action or a complex `Collection` with multiple actions.
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

    private TypeLibraryDTO createEmptyArcLibrary(AasTargetApiRequestConfiguration arc) {
        String clientClassName = toPascalCase(arc.alias) + "_Client";
        String content = String.format("declare class %s {}\n", clientClassName);
        String filePath = "stayinsync/targets/aas/" + arc.alias + ".d.ts";
        return new TypeLibraryDTO(filePath, content);
    }

    // --- Helper Methods ---

    private String mapValueTypeToTs(String valueType) {
        if (valueType == null || valueType.isBlank()) return "any";
        return switch (valueType.toLowerCase()) {
            case "xs:string", "string" -> "string";
            case "xs:double", "xs:int", "xs:integer", "xs:long", "xs:float", "xs:decimal", "number" -> "number";
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

    private String toPascalCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return Arrays.stream(s.split("[_\\- ]"))
                .map(this::capitalize)
                .collect(Collectors.joining());
    }

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