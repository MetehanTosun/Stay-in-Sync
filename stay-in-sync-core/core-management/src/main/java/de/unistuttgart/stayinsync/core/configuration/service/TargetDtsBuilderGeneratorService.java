package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfigurationAction;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.GetTypeDefinitionsResponseDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.TypeLibraryDTO;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTargetDtsGeneratorService;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;
import io.quarkus.cache.CacheResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This service is the core engine responsible for generating all TypeScript Declaration Files (.d.ts)
 * for the 'targets' object in the script editor. It orchestrates the entire generation process for both
 * REST and AAS Target ARCs associated with a transformation.
 * <p>
 * The primary goal is to provide a strongly-typed development experience for the end-user, enabling
 * rich autocompletion and type-checking within the Monaco editor. It achieves this by:
 * <ul>
 *   <li>Parsing OpenAPI specifications for REST-based target systems.</li>
 *   <li>Generating TypeScript interfaces ({@code declare interface}) for all shared data models (schemas).</li>
 *   <li>Creating a fluent "Builder" pattern for each REST ARC, allowing users to construct API calls in a guided, type-safe manner.</li>
 *   <li>Integrating with {@link AasTargetDtsGeneratorService} to generate corresponding builders for AAS ARCs.</li>
 *   <li>Assembling all generated types into a set of interconnected "library" files, including a global manifest (targets)
 *       and a contract (DirectiveMap) that defines the script's required output structure.</li>
 * </ul>
 */
@ApplicationScoped
public class TargetDtsBuilderGeneratorService {

    @Inject
    AasTargetDtsGeneratorService aasTargetDtsGeneratorService;

    /**
     * The main entry point for the service. It orchestrates the end-to-end generation of all necessary
     * .d.ts files for a given transformation. It gathers all active REST and AAS ARCs, generates their respective
     * type libraries, and assembles them along with shared libraries (base, contract, manifest) into a single response DTO.
     *
     * @param transformationId The ID of the transformation for which to generate the type definitions.
     * @return A {@link GetTypeDefinitionsResponseDTO} containing a list of all generated {@link TypeLibraryDTO} files.
     * @throws CoreManagementException if the transformation is not found.
     */
    public GetTypeDefinitionsResponseDTO generateForTransformation(Long transformationId) {
        Transformation transformation = Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Transformation not found", "Transformation with id %d not found.", transformationId));

        Set<TargetSystemApiRequestConfiguration> restArcs = transformation.targetSystemApiRequestConfigurations;

        Map<Long, OpenAPI> parsedSpecs = getParsedOpenApiSpecifications(restArcs);
        DtsGenerationContext restContext = new DtsGenerationContext();
        TypeLibraryDTO sharedModelsLibrary = generateSharedModelsLibrary(parsedSpecs, restContext);
        final boolean hasSharedModels = sharedModelsLibrary != null && !sharedModelsLibrary.content().isEmpty();

        List<TypeLibraryDTO> restArcLibraries = restArcs.stream()
                .map(arc -> generateArcLibrary(arc, parsedSpecs.get(arc.targetSystem.id), restContext, hasSharedModels))
                .toList();

        Set<AasTargetApiRequestConfiguration> aasArcs = transformation.aasTargetApiRequestConfigurations;
        List<TypeLibraryDTO> aasArcLibraries = aasTargetDtsGeneratorService.generateForAasArcs(aasArcs);

        String globalNamespace = generatedGlobalTargetsNamespace(restArcs, aasArcs);
        TypeLibraryDTO manifestLibrary = new TypeLibraryDTO("stayinsync/targets/manifest.d.ts", globalNamespace);
        TypeLibraryDTO baseDirectiveLibrary = new TypeLibraryDTO("stayinsync/targets/base.d.ts",
                "/** A shared base interface for all generated target directives. */\ndeclare interface TargetDirective {}");
        TypeLibraryDTO contractLibrary = generateContractLibrary(restArcs, aasArcs);

        List<TypeLibraryDTO> allLibraries = new ArrayList<>();
        allLibraries.add(baseDirectiveLibrary);
        allLibraries.add(contractLibrary);
        if (sharedModelsLibrary != null) {
            allLibraries.add(sharedModelsLibrary);
        }
        allLibraries.addAll(restArcLibraries);
        allLibraries.addAll(aasArcLibraries);
        allLibraries.add(manifestLibrary);

        return new GetTypeDefinitionsResponseDTO(allLibraries);
    }

    /**
     * Parses a string containing an OpenAPI 3 specification into a structured {@link OpenAPI} object.
     * This method is cached using {@code @CacheResult} to avoid re-parsing the same specification content multiple times,
     * which is a computationally expensive operation.
     *
     * @param specificationContent The raw string content of the OpenAPI specification.
     * @return The parsed {@link OpenAPI} object model.
     * @throws CoreManagementException if the specification content is invalid and cannot be parsed.
     */
    @CacheResult(cacheName = "openapi-specs-cache")
    public OpenAPI parseSpecification(String specificationContent) {
        try {
            return new OpenAPIV3Parser().readContents(specificationContent).getOpenAPI();
        } catch (Exception e) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid OpenAPI specification",
                    "Failed to parse OpenAPI specification: %s", e.getMessage());
        }
    }

    /**
     * Gathers all unique target systems from a set of REST ARCs and parses their OpenAPI specifications.
     *
     * @param arcs A set of {@link TargetSystemApiRequestConfiguration} for which to parse specs.
     * @return A map where the key is the target system ID and the value is the parsed {@link OpenAPI} object.
     */
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

    /**
     * Generates a single .d.ts file containing TypeScript interfaces for all shared schemas (components.schemas)
     * found across multiple OpenAPI specifications. This creates a common library of data models (e.g., Product, Customer)
     * that can be referenced by multiple ARC-specific builders, promoting code reuse and consistency.
     *
     * @param parsedSpecifications A map of target system IDs to their parsed {@link OpenAPI} objects.
     * @param context              The {@link DtsGenerationContext} to track already generated model names and prevent duplicates.
     * @return A {@link TypeLibraryDTO} representing the {@code stayinsync/shared/models.d.ts} file.
     */
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

    /**
     * Generates a dedicated .d.ts file for a single REST Target ARC. This file contains all the necessary
     * TypeScript declarations for that ARC's fluent builder pattern, including:
     * <ul>
     *   <li>A series of chained builder interfaces for setting required and optional parameters.</li>
     *   <li>The main builder interface (e.g., {@code SynchronizeProducts_UpsertBuilder}).</li>
     *   <li>The final directive type (e.g., {@code SynchronizeProducts_UpsertDirective}).</li>
     *   <li>The client class (e.g., {@code SynchronizeProducts_Client}) that serves as the entry point.</li>
     * </ul>
     *
     * @param arc             The {@link TargetSystemApiRequestConfiguration} to generate the library for.
     * @param specification   The parsed {@link OpenAPI} specification for the ARC's target system.
     * @param context         The {@link DtsGenerationContext} for tracking generated types.
     * @param hasSharedModels A boolean indicating if a {@code /// <reference ...>} path to the shared models library should be included.
     * @return A {@link TypeLibraryDTO} representing the ARC-specific file (e.g., {@code stayinsync/targets/arcs/synchronizeProducts.d.ts}).
     */
    private TypeLibraryDTO generateArcLibrary(TargetSystemApiRequestConfiguration arc, OpenAPI specification, DtsGenerationContext context, boolean hasSharedModels) {
        StringBuilder dtsContent = new StringBuilder();

        if (hasSharedModels) {
            dtsContent.append("/// <reference path=\"../shared/models.d.ts\" />\n\n");
        }

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

    /**
     * Generates the content for the {@code stayinsync/targets/manifest.d.ts} file. This file declares the global {@code targets} constant,
     * which acts as the main namespace and entry point for users in the script editor. It populates this object with a
     * property for each active REST and AAS ARC, typed to its corresponding client class.
     * <p><b>Example Output:</b></p>
     * <pre>{@code
     * declare const targets: {
     *   /&#42;&#42; ARC for REST target 'synchronizeProducts'. &#42;/
     *   synchronizeProducts: SynchronizeProducts_Client;
     *   /&#42;&#42; ARC for AAS target 'updateAasSubmodel'. &#42;/
     *   updateAasSubmodel: UpdateAasSubmodel_Client;
     * }
     * }</pre>
     *
     * @param restArcs The set of active REST ARCs.
     * @param aasArcs  The set of active AAS ARCs.
     * @return A string containing the full content of the manifest file.
     */
    private String generatedGlobalTargetsNamespace(Set<TargetSystemApiRequestConfiguration> restArcs, Set<AasTargetApiRequestConfiguration> aasArcs) {
        StringBuilder content = new StringBuilder();

        if (restArcs != null) {
            restArcs.forEach(arc ->
                    content.append(String.format("  /** ARC for REST target '%s'. */\n", arc.alias))
                            .append(String.format("  %s: %s_Client;\n", arc.alias, toPascalCase(arc.alias)))
            );
        }

        if (aasArcs != null) {
            aasArcs.forEach(arc ->
                    content.append(String.format("\n  /** ARC for AAS target '%s' (Submodel: %s). */\n", arc.alias, arc.submodel.submodelIdShort))
                            .append(String.format("  %s: %s_Client;\n", arc.alias, toPascalCase(arc.alias)))
            );
        }

        return String.format(
                """
                        /**
                         * Global object providing access to all configured Target ARCs.
                         * Use this to define the instructions (Directives) for your target systems.
                         */
                        declare const targets: {
                        %s}""",
                content
        );
    }

    /**
     * Generates the main builder interface for a REST ARC's upsert pattern (e.g., {@code SynchronizeProducts_UpsertBuilder}).
     * This interface exposes the {@code usingCheck()}, {@code usingCreate()}, and {@code usingUpdate()} methods, along with the final {@code build()} method.
     *
     * @param arc The ARC for which to generate the builder interface.
     * @return A string containing the TypeScript {@code declare interface} block.
     */
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

    /**
     * Generates the final directive type interface for an ARC (e.g., {@code SynchronizeProducts_UpsertDirective}). This is an
     * opaque "marker" interface that extends the base {@code TargetDirective}. Its purpose is to be the return type of the
     * builder's {@code build()} method, ensuring type safety in the {@code DirectiveMap}.
     *
     * @param arc The ARC for which to generate the directive interface.
     * @return A string containing the TypeScript {@code declare interface} block.
     */
    private String generateDirectiveInterfaces(TargetSystemApiRequestConfiguration arc) {
        return String.format("declare interface %s extends TargetDirective { /* Internal recipe type for the executor */ }\n", toPascalCase(arc.alias) + "_UpsertDirective");
    }

    /**
     * Generates a chain of builder interfaces for a single action (e.g., CHECK, CREATE, UPDATE). This is the core
     * of the fluent builder pattern. It creates a sequence of interfaces where each interface has exactly one method for
     * a required parameter, which then returns the next interface in the chain. This forces the user to provide all
     * required parameters in a specific order. Optional parameters are added to the final interface in the chain.
     *
     * @param action        The action (CHECK, CREATE, or UPDATE) to generate builders for.
     * @param specification The relevant {@link OpenAPI} specification.
     * @param context       The {@link DtsGenerationContext}.
     * @return A string containing all the chained {@code declare interface} blocks for this action.
     */
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

    /**
     * Appends methods for all optional parameters and a generic {@code withCustomQueryParam} method to the final
     * builder interface in a chain. It also handles adding an optional {@code withPayload} method if the endpoint's
     * request body is not marked as required.
     *
     * @param sb                 The {@link StringBuilder} to append the method signatures to.
     * @param interfaceName      The name of the final interface (e.g., {@code CheckBuilder_Final}).
     * @param optionalParameters The list of optional {@link Parameter}s for the endpoint.
     * @param operation          The OpenAPI {@link Operation} object for the endpoint.
     * @param specification      The {@link OpenAPI} specification.
     * @param context            The {@link DtsGenerationContext}.
     * @param action             The current action being processed.
     */
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

    /**
     * Generates the full TypeScript method signature for a single parameter in a builder interface (e.g., {@code withQueryParamId(value: number): NextBuilder}).
     * It has special logic for path parameters in an UPDATE action, generating a signature that accepts a callback function
     * {@code (checkResponse: CheckResponseType) => number} to dynamically source the ID from the result of the CHECK action.
     *
     * @param parameter         The OpenAPI {@link Parameter} object.
     * @param nextInterfaceName The name of the interface that this method should return.
     * @param action            The current action being processed.
     * @param isOptional        A boolean indicating if the parameter is optional, which adds a {@code ?} to the method name.
     * @param specification     The {@link OpenAPI} specification.
     * @param context           The {@link DtsGenerationContext}.
     * @return A string containing the complete method signature.
     */
    private String generateMethodSignature(Parameter parameter, String nextInterfaceName,
                                           TargetSystemApiRequestConfigurationAction action, boolean isOptional, OpenAPI specification, DtsGenerationContext context) {
        String methodName = "with" + toPascalCase(parameter.getIn()) + "Param" + toPascalCase(parameter.getName());
        String paramType = extractTsTypeFromSchema(parameter.getSchema(), specification, context);
        String optionalMarker = isOptional ? "?" : "";

        if (action.actionRole == TargetApiRequestConfigurationActionRole.UPDATE && "path".equals(parameter.getIn())) {
            String checkResponseType = extractCheckResponseTypeName(action, specification, context);
            if (checkResponseType != null) {
                return String.format("%s%s(idProvider: (checkResponse: %s) => %s): %s",
                        methodName, optionalMarker, checkResponseType, paramType, nextInterfaceName);
            }
        }

        return String.format("%s%s(value: %s): %s", methodName, optionalMarker, paramType, nextInterfaceName);
    }

    /**
     * Finds the corresponding OpenAPI {@link Operation} object within a parsed specification based on an endpoint's path and HTTP method.
     *
     * @param endpoint      The {@link SyncSystemEndpoint} to find.
     * @param specification The {@link OpenAPI} specification to search within.
     * @return The found {@link Operation} object, or {@code null} if not found.
     */
    private Operation findOperation(SyncSystemEndpoint endpoint, OpenAPI specification) {
        if (specification == null || specification.getPaths() == null) {
            return null;
        }

        PathItem pathItem = specification.getPaths().get(endpoint.endpointPath);
        if (pathItem == null) {
            return null;
        }

        return switch (endpoint.httpRequestType.toUpperCase()) {
            case "GET" -> pathItem.getGet();
            case "POST" -> pathItem.getPost();
            case "PUT" -> pathItem.getPut();
            case "DELETE" -> pathItem.getDelete();
            case "PATCH" -> pathItem.getPatch();
            default -> null;
        };
    }

    /**
     * Extracts the TypeScript type name for an endpoint's request body (application/json).
     * It resolves {@code $ref} pointers to shared schemas or derives the type directly.
     *
     * @param requestBody   The OpenAPI {@link RequestBody} object.
     * @param specification The {@link OpenAPI} specification.
     * @param context       The {@link DtsGenerationContext}.
     * @return The TypeScript type name (e.g., "Product", "any").
     */
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

    /**
     * Specifically for UPDATE actions, this method finds the associated CHECK action within the same ARC and
     * determines the TypeScript type name of its successful (2xx) JSON response. This is used to strongly type the
     * {@code checkResponse} parameter in the {@code idProvider} callback for path parameters.
     *
     * @param updateAction  The UPDATE action.
     * @param specification The {@link OpenAPI} specification.
     * @param context       The {@link DtsGenerationContext}.
     * @return The TypeScript type name of the CHECK action's response (e.g., "Products").
     */
    private String extractCheckResponseTypeName(TargetSystemApiRequestConfigurationAction updateAction, OpenAPI specification, DtsGenerationContext context) {
        Optional<TargetSystemApiRequestConfigurationAction> checkAction = updateAction.targetSystemApiRequestConfiguration.actions.stream()
                .filter(action -> action.actionRole == TargetApiRequestConfigurationActionRole.CHECK)
                .findFirst();

        if (checkAction.isEmpty()) {
            return "any /* No CHECK action defined in this ARC */";
        }

        Operation checkOperation = findOperation(checkAction.get().endpoint, specification);

        if (checkOperation == null) {
            return "any /* CHECK operation could not be found in the OpenAPI spec */";
        }

        ApiResponse successResponse = null;
        for (Map.Entry<String, ApiResponse> entry : checkOperation.getResponses().entrySet()) {
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

    /**
     * Generates a TypeScript {@code declare interface} block from an OpenAPI {@link Schema} object. It handles properties,
     * required fields, and {@code allOf} composition to correctly represent the data model.
     *
     * @param interfaceName The desired name for the TypeScript interface.
     * @param schema        The OpenAPI {@link Schema} object to convert.
     * @param specification The {@link OpenAPI} specification (for resolving {@code $ref}).
     * @param context       The {@link DtsGenerationContext}.
     * @return A string containing the complete {@code declare interface} definition.
     */
    private String generateInterfaceFromSchema(String interfaceName, Schema<?> schema, OpenAPI specification, DtsGenerationContext context) {
        StringBuilder currentInterface = new StringBuilder();
        Map<String, Schema> allProperties = new LinkedHashMap<>();
        List<String> allRequired = new ArrayList<>();

        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            for (Schema<?> partSchema : schema.getAllOf()) {
                if (partSchema.get$ref() != null) {
                    String refName = partSchema.get$ref().substring(partSchema.get$ref().lastIndexOf('/') + 1);
                    Schema<?> resolvedSchema = specification.getComponents().getSchemas().get(refName);
                    if (resolvedSchema != null) {
                        if (resolvedSchema.getProperties() != null) {
                            allProperties.putAll(resolvedSchema.getProperties());
                        }
                        if (resolvedSchema.getRequired() != null) {
                            allRequired.addAll(resolvedSchema.getRequired());
                        }
                    }
                } else {
                    if (partSchema.getProperties() != null) {
                        allProperties.putAll(partSchema.getProperties());
                    }
                    if (partSchema.getRequired() != null) {
                        allRequired.addAll(partSchema.getRequired());
                    }
                }
            }
        } else {
            if (schema.getProperties() != null) {
                allProperties.putAll(schema.getProperties());
            }
            if (schema.getRequired() != null) {
                allRequired.addAll(schema.getRequired());
            }
        }

        currentInterface.append(String.format("declare interface %s {\n", interfaceName));

        if (!allProperties.isEmpty()) {
            allProperties.forEach((fieldName, fieldSchema) -> {
                String validFieldName = fieldName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$") ? fieldName : String.format("'%s'", fieldName);
                String type = extractTsTypeFromSchema(fieldSchema, specification, context);
                boolean isRequired = allRequired.contains(fieldName);

                currentInterface.append(String.format("  %s%s: %s;\n",
                        validFieldName,
                        isRequired ? "" : "?",
                        type));
            });
        }

        currentInterface.append("}\n");
        return currentInterface.toString();
    }

    /**
     * Recursively extracts a TypeScript type from an OpenAPI {@link Schema} object. It handles various schema types, including
     * {@code $ref} pointers, enums, arrays, objects, and primitive types. For inline objects, it can generate an anonymous interface.
     *
     * @param schema        The {@link Schema} object.
     * @param specification The {@link OpenAPI} specification.
     * @param context       The {@link DtsGenerationContext}.
     * @return The corresponding TypeScript type as a string (e.g., "string", "number[]", "Product").
     */
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

    /**
     * Generates the content for the {@code stayinsync/targets/contract.d.ts} file. This is one of the most important generated files as it defines:
     * <ol>
     *   <li>The {@code DirectiveMap} interface, which specifies the exact shape of the object that the user's {@code transform()} function must return.</li>
     *   <li>The {@code declare function transform(): DirectiveMap;} signature, which tells the TypeScript compiler about the global entry point function.</li>
     * </ol>
     * This enforces the primary contract between the user's script and the execution engine.
     *
     * @param restArcs The set of active REST ARCs.
     * @param aasArcs  The set of active AAS ARCs.
     * @return A {@link TypeLibraryDTO} containing the content of the contract file.
     */
    private TypeLibraryDTO generateContractLibrary(Set<TargetSystemApiRequestConfiguration> restArcs, Set<AasTargetApiRequestConfiguration> aasArcs) {
        StringBuilder content = new StringBuilder();

        // --- DirectiveMap Interface declaration ---
        content.append("/**\n");
        content.append(" * Defines the required structure for the return value of the 'transform' function.\n");
        content.append(" * Each key MUST match the alias of a configured Target ARC.\n");
        content.append(" * The value for each key MUST be an array of the corresponding directive types.\n");
        content.append(" */\n");
        content.append("declare interface DirectiveMap {\n");

        if (restArcs != null) {
            for (TargetSystemApiRequestConfiguration arc : restArcs) {
                String alias = arc.alias;
                String directiveTypeName = toPascalCase(alias) + "_UpsertDirective";
                content.append(String.format("  /** Directives for the REST ARC '%s'. */\n", alias));
                content.append(String.format("  %s: %s[];\n", alias, directiveTypeName));
            }
        }

        if (aasArcs != null) {
            for (AasTargetApiRequestConfiguration arc : aasArcs) {
                String alias = arc.alias;
                String directiveBaseName = toPascalCase(alias);
                String unionType = String.format("(%s_SetValueDirective | %s_AddElementDirective | %s_DeleteElementDirective)",
                        directiveBaseName, directiveBaseName, directiveBaseName);

                content.append(String.format("\n  /** Directives for the AAS ARC '%s' (Submodel: %s). */\n", alias, arc.submodel.submodelIdShort));
                content.append(String.format("  %s: %s[];\n", alias, unionType));
            }
        }

        content.append("}\n\n");

        // --- transform() function-signature declaration ---
        content.append("/**\n");
        content.append(" * This is the main entry point for your transformation logic.\n");
        content.append(" * It MUST return an object that conforms to the DirectiveMap interface.\n");
        content.append(" */\n");
        content.append("declare function transform(): DirectiveMap;\n");

        return new TypeLibraryDTO("stayinsync/targets/contract.d.ts", content.toString());
    }

    /**
     * Maps a primitive OpenAPI data type string to its TypeScript equivalent.
     *
     * @param openApiType The OpenAPI type string (e.g., "integer", "string").
     * @return The corresponding TypeScript type string (e.g., "number", "string").
     */
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

    /**
     * A utility function to convert a string from any case (snake_case, kebab-case, space-separated) to PascalCase.
     *
     * @param s The input string.
     * @return The PascalCase version of the string.
     */
    private String toPascalCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        return Arrays.stream(s.split("[_\\- ]"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining());
    }

    /**
     * A utility function to convert a SCREAMING_SNAKE_CASE enum name to PascalCase.
     *
     * @param enumName The input enum name string.
     * @return The PascalCase version of the string.
     */
    private String enumNameToPascalCase(String enumName) {
        if (enumName == null || enumName.isEmpty()) {
            return enumName;
        }
        return Arrays.stream(enumName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining());
    }

    /**
     * A simple inner class used as a stateful context object during the type generation process.
     * Its primary purpose is to hold a set of model names that have already been generated to prevent
     * duplicate interface definitions in the shared models library.
     */
    private static class DtsGenerationContext {
        private final Set<String> generatedModelNames = new HashSet<>();
    }
}
