package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemApiRequestConfigurationAction;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.GetTypeDefinitionsResponseDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.TypeLibraryDTO;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class TargetDtsBuilderGeneratorService {

    public GetTypeDefinitionsResponseDTO generateForTransformation(Long transformationId) {
        Transformation transformation = Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Transformation not found", "Transformation with id %d not found.", transformationId));

        Set<TargetSystemApiRequestConfiguration> targetArcs = transformation.targetSystemApiRequestConfigurations;

        Map<Long, OpenAPI> parsedSpecs = getParsedOpenApiSpecifications(targetArcs);
        DtsGenerationContext context = new DtsGenerationContext();

        TypeLibraryDTO sharedModelsLibrary = generateSharedModelsLibrary(parsedSpecs, context);
        List<TypeLibraryDTO> arcLibraries = targetArcs.stream()
                .map(arc -> generateArcLibrary(arc, parsedSpecs.get(arc.targetSystem.id), context))
                .toList();

        String globalNamespace = generatedGlobalTargetsNamespace(targetArcs);
        TypeLibraryDTO manifestLibrary = new TypeLibraryDTO("stayinsync/targets/manifest.d.ts", globalNamespace);

        List<TypeLibraryDTO> allLibraries = new ArrayList<>();
        if (sharedModelsLibrary != null && !sharedModelsLibrary.content().isEmpty()) {
            allLibraries.add(sharedModelsLibrary);
        }
        allLibraries.addAll(arcLibraries);
        allLibraries.add(manifestLibrary);

        return new GetTypeDefinitionsResponseDTO(allLibraries);
    }

    @CacheResult(cacheName = "openapi-specs-cache")
    public OpenAPI parseSpecification(String specificationContent) {
        try {
            return new OpenAPIV3Parser().readContents(specificationContent).getOpenAPI();
        } catch (Exception e) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid OpenAPI specification",
                    "Failed to parse OpenAPI specification: %s", e.getMessage());
        }
    }

    private Map<Long, OpenAPI> getParsedOpenApiSpecifications(Set<TargetSystemApiRequestConfiguration> arcs) {
        return arcs.stream()
                .map(arc -> arc.targetSystem)
                .distinct()
                .collect(Collectors.toMap(
                        system -> system.id,
                        system -> parseSpecification(system.openApiSpec),
                        (existing, replacement) -> existing
                ));
    }

    private TypeLibraryDTO generateSharedModelsLibrary(Map<Long, OpenAPI> parsedSpecifications, DtsGenerationContext context) {
        StringBuilder content = new StringBuilder();

        parsedSpecifications.values().forEach(specification -> {
            if (specification.getComponents() != null && specification.getComponents().getSchemas() != null) {
                specification.getComponents().getSchemas().forEach((schemaName, schema) -> {
                    if (!context.generatedModelNames.contains(schemaName)) {
                        content.append(generateInterfaceFromSchema(schemaName, schema, specification, context));
                        context.generatedModelNames.add(schemaName);
                    }
                });
            }
        });
        return new TypeLibraryDTO("stayinsync/shared/models.d.ts", content.toString());
    }

    private TypeLibraryDTO generateArcLibrary(TargetSystemApiRequestConfiguration arc, OpenAPI specification, DtsGenerationContext context) {
        StringBuilder dtsContent = new StringBuilder();
        String clientClassName = toPascalCase(arc.alias) + "_Client";
        String builderName = toPascalCase(arc.alias) + "_UpsertBuilder";

        for (TargetSystemApiRequestConfigurationAction action : arc.actions) {
            dtsContent.append(generateBuilderInterfacesForAction(action, specification, context));
        }

        dtsContent.append(generateMainUpsertBuilder(arc));
        dtsContent.append(generateDirectiveInterfaces(arc));
        dtsContent.append(String.format("declare class %s { defineUpsert(): %s; }\n", clientClassName, builderName));

        String filePath = "stayinsync/targets/arcs/" + arc.alias + ".d.ts";
        return new TypeLibraryDTO(filePath, dtsContent.toString());
    }

    private String generatedGlobalTargetsNamespace(Set<TargetSystemApiRequestConfiguration> arcs) {
        if (arcs == null || arcs.isEmpty()) {
            return "";
        }

        String content = arcs.stream()
                .map(arc -> String.format("  %s: %s_Client;", arc.alias, toPascalCase(arc.alias)))
                .collect(Collectors.joining("\n"));

        return String.format("declare const targets: {\n%s\n}", content);
    }

    private String generateMainUpsertBuilder(TargetSystemApiRequestConfiguration arc) {
        String builderName = toPascalCase(arc.alias) + "_UpsertBuilder";
        String directiveName = toPascalCase(arc.alias) + "_UpsertDirective";
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("declare interface %s {\n", builderName));

        for (TargetSystemApiRequestConfigurationAction action : arc.actions) {
            String roleName = enumNameToPascalCase(action.actionRole.name());
            String initialBuilderInterface = roleName + "Builder_Initial";
            builder.append(String.format("  using%s(config: (builder: %s) => void): this;\n", roleName, initialBuilderInterface));
        }

        builder.append(String.format("  build(): %s;\n", directiveName));
        builder.append("}\n");

        return builder.toString();
    }

    private String generateDirectiveInterfaces(TargetSystemApiRequestConfiguration arc) {
        return String.format("declare interface %s { /* Internal recipe type for the executor */ }\n", toPascalCase(arc.alias) + "_UpsertDirective");
    }

    private String generateBuilderInterfacesForAction(TargetSystemApiRequestConfigurationAction action, OpenAPI specification, DtsGenerationContext context) {
        Operation operation = findOperation(action.endpoint, specification);
        if (operation == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "Endpoint not found.",
                    "The OpenAPI Spec for " + action.endpoint.syncSystem.name + " does not contain an endpoint Definition for " + action.endpoint.endpointPath);
        }

        List<Parameter> allParams = Optional.ofNullable(operation.getParameters()).orElse(new ArrayList<>());
        List<Parameter> requiredParams = allParams.stream().filter(p -> Boolean.TRUE.equals(p.getRequired())).toList();
        List<Parameter> optionalParams = allParams.stream().filter(p -> !Boolean.TRUE.equals(p.getRequired())).toList();

        final boolean isPayloadRequired = operation.getRequestBody() != null && Boolean.TRUE.equals(operation.getRequestBody().getRequired());

        StringBuilder interfaces = new StringBuilder();
        String baseName = enumNameToPascalCase(action.actionRole.name());
        String previousInterfaceName = baseName + "Builder_Initial";

        for (int i = 0; i < requiredParams.size(); i++) {
            Parameter param = requiredParams.get(i);
            String nextInterfaceName;

            if (i + 1 < requiredParams.size()) {
                // Generate Builders for required parameters
                nextInterfaceName = baseName + "Builder_With" + toPascalCase(requiredParams.get(i + 1).getName());
            } else if (isPayloadRequired) {
                // If payload is required, put the Builder after required parameters
                nextInterfaceName = baseName + "Builder_WithPayload";
            } else {
                // optional methods
                nextInterfaceName = baseName + "Builder_Final";
            }

            interfaces.append(String.format("declare interface %s {\n", previousInterfaceName));
            interfaces.append(String.format("  %s;\n", generateMethodSignature(param, nextInterfaceName, action, false, specification, context)));
            interfaces.append("}\n");
            previousInterfaceName = nextInterfaceName;
        }

        if (isPayloadRequired) {
            String payloadInterfaceName = requiredParams.isEmpty() ? baseName + "Builder_Initial" : baseName + "Builder_WithPayload";
            String finalInterfaceName = baseName + "Builder_Final";

            interfaces.append(String.format("declare interface %s {\n", payloadInterfaceName));
            String payloadType = extractPayloadTypeName(operation.getRequestBody(), specification, context);
            interfaces.append(String.format("  withPayload(data: %s): %s;\n", payloadType, finalInterfaceName));
            interfaces.append("}\n");
            previousInterfaceName = finalInterfaceName;
        }

        final String finalInterfaceName = previousInterfaceName;
        interfaces.append(String.format("declare interface %s {\n", finalInterfaceName));
        appendOptionalMethods(interfaces, finalInterfaceName, optionalParams, operation, specification, context, action);
        interfaces.append("}\n");

        return interfaces.toString();
    }

    private void appendOptionalMethods(StringBuilder sb, String interfaceName, List<Parameter> optionalParameters,
                                       Operation operation, OpenAPI specification, DtsGenerationContext context, TargetSystemApiRequestConfigurationAction action) {
        boolean isOptional = true;

        optionalParameters.forEach(param -> sb.append(String.format("  %s;\n", generateMethodSignature(param, interfaceName, action, isOptional, specification, context))));

        sb.append(String.format("  withCustomQueryParam?(key: string, value: any): %s;\n", interfaceName));

        // Specific handling in case payloads are optional
        if (operation.getRequestBody() != null && !Boolean.TRUE.equals(operation.getRequestBody().getRequired())) {
            String payloadType = extractPayloadTypeName(operation.getRequestBody(), specification, context);
            sb.append(String.format("  withPayload?(data: %s): %s;\n", payloadType, interfaceName));
        }
    }

    private String generateMethodSignature(Parameter parameter, String nextInterfaceName,
                                           TargetSystemApiRequestConfigurationAction action, boolean isOptional, OpenAPI specification, DtsGenerationContext context) {
        String methodName = "with" + toPascalCase(parameter.getIn()) + "Param" + toPascalCase(parameter.getName());
        String paramType = extractTsTypeFromSchema(parameter.getSchema(), specification, context);
        String optionalMarker = isOptional ? "?" : "";

        if (action.actionRole == TargetApiRequestConfigurationActionRole.UPDATE && "path".equals(parameter.getIn())){
            String checkResponseType = extractCheckResponseTypeName(action, specification, context);
            if (checkResponseType != null) {
                return String.format("%s%s(idProvider: (checkResponse: %s) => %s): %s",
                        methodName, optionalMarker, checkResponseType, paramType, nextInterfaceName);
            }
        }

        return String.format("%s%s(value: %s): %s", methodName, optionalMarker, paramType, nextInterfaceName);
    }

    private Operation findOperation(SyncSystemEndpoint endpoint, OpenAPI specification) {
        if (specification == null || specification.getPaths() == null) {
            return null;
        }

        PathItem pathItem = specification.getPaths().get(endpoint.endpointPath);
        if (pathItem == null) {
            return null;
        }

        return switch(endpoint.httpRequestType.toUpperCase()){
            case "GET" -> pathItem.getGet();
            case "POST" -> pathItem.getPost();
            case "PUT" -> pathItem.getPut();
            case "DELETE" -> pathItem.getDelete();
            case "PATCH" -> pathItem.getPatch();
            default -> null;
        };
    }

    private String extractPayloadTypeName(RequestBody requestBody, OpenAPI specification, DtsGenerationContext context) {
        if (requestBody.get$ref() != null) {
            String ref = requestBody.get$ref();
            return ref.substring(ref.lastIndexOf('/') + 1);
        }

        if (requestBody.getContent() != null && requestBody.getContent().containsKey("application/json")) {
            Schema<?> schema = requestBody.getContent().get("application/json").getSchema();
            return extractTsTypeFromSchema(schema, specification, context);
        }

        return "any";
    }

    private String extractCheckResponseTypeName(TargetSystemApiRequestConfigurationAction updateAction,OpenAPI specification, DtsGenerationContext context) {
        Optional<TargetSystemApiRequestConfigurationAction> checkAction = updateAction.targetSystemApiRequestConfiguration.actions.stream()
                .filter(action -> action.actionRole == TargetApiRequestConfigurationActionRole.CHECK)
                .findFirst();

        if(checkAction.isEmpty()) {
            return "any /* No CHECK action defined in this ARC */";
        }

        Operation checkOperation = findOperation(checkAction.get().endpoint, specification);

        if( checkOperation == null) {
            return "any /* CHECK operation could not be found in the OpenAPI spec */";
        }

        ApiResponse successResponse = null;
        for(Map.Entry<String, ApiResponse> entry : checkOperation.getResponses().entrySet()){
            if (entry.getKey().startsWith("2")) {
                successResponse = entry.getValue();
                break;
            }
        }

        if (successResponse == null) {
            return "any /* No 2xx success response found for CHECK operation */";
        }

        if (successResponse.getContent() != null && successResponse.getContent().containsKey("application/json")) {
            Schema<?> schema = successResponse.getContent().get("application/json").getSchema();
            return extractTsTypeFromSchema(schema, specification, context);
        }

        return "any /* No JSON response content */";
    }

    private String generateInterfaceFromSchema(String interfaceName, Schema<?> schema, OpenAPI specification, DtsGenerationContext context) {
        StringBuilder currentInterface = new StringBuilder();
        currentInterface.append(String.format("declare interface %s {\n", interfaceName));

        if (schema.getProperties() != null){
            schema.getProperties().forEach((fieldName, value) -> {
                String validFieldName = fieldName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$") ? fieldName : String.format("'%s'", fieldName);
                Schema<?> fieldSchema = (Schema<?>) value;
                String type = extractTsTypeFromSchema(fieldSchema, specification, context);
                boolean isRequired = schema.getRequired() != null && schema.getRequired().contains(fieldName);

                currentInterface.append(String.format("  %s%s: %s;\n",
                        validFieldName,
                        isRequired ? "" : "?",
                        type));
            });
        }

        currentInterface.append("}\n");
        return currentInterface.toString();
    }

    private String extractTsTypeFromSchema(Schema<?> schema, OpenAPI specification, DtsGenerationContext context) {
        if (schema == null) return "any";

        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            return ref.substring(ref.lastIndexOf('/') + 1);
        }

        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return schema.getEnum().stream()
                    .map(Object::toString)
                    .map(e -> "\"" + e + "\"")
                    .collect(Collectors.joining(" | "));
        }

        if ("array".equals(schema.getType())) {
            String itemsType = extractTsTypeFromSchema(schema.getItems(), specification, context);
            return itemsType == null ? "any[]" : itemsType + "[]";
        }

        if ("object".equals(schema.getType())) {
            if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                String interfaceName = "Anonymous_" + context.generatedModelNames.size();
                context.generatedModelNames.add(interfaceName);
                return generateInterfaceFromSchema(interfaceName, schema, specification, context).trim();
            }
            return "{ [key: string]: any }";
        }

        return mapOpenApiToTsType(schema.getType());
    }

    private String mapOpenApiToTsType(String openApiType) {
        if (openApiType == null) {
            return "any";
        }
        return switch (openApiType) {
            case "string" -> "string";
            case "number", "integer", "float", "double" -> "number";
            case "boolean" -> "boolean";
            case "array" -> "any[]";
            case "object" -> "{ [key: string]: any }";
            default -> "any";
        };
    }

    private String toPascalCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String enumNameToPascalCase(String enumName) {
        if (enumName == null || enumName.isEmpty()) {
            return enumName;
        }
        return Arrays.stream(enumName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining());
    }

    private static class DtsGenerationContext {
        private final Set<String> generatedModelNames = new HashSet<>();
    }
}
