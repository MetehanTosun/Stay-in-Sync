package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfigurationAction;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates the executable JavaScript SDK for a given Transformation.
 * This SDK creates the 'targets' object within the GraalJS runtime context, providing a fluent API
 * for building directives based on the configured Target ARCs. The generated script is intended to be
 * persisted with the Transformation entity and executed at runtime.
 * <p>
 * The core responsibility of this service is to translate the declarative configuration of REST-based
 * Target ARCs into a functional, imperative JavaScript API that is intuitive for the script-writer.
 * It dynamically creates classes and methods that correspond to the actions and parameters defined
 * <p>
 * in the associated OpenAPI specifications.
 */
@ApplicationScoped
public class TargetSdkGeneratorService {

    @Inject
    TargetDtsBuilderGeneratorService dtsGeneratorService;

    /**
     * Generates the full JavaScript SDK script for a given Transformation entity.
     * This is the main entry point of the service. It constructs the entire script by initializing
     * the global `targets` object and then iterating through each configured REST Target ARC,
     * appending the SDK snippet for each one.
     *
     * @param transformation The Transformation entity containing the Target ARC configurations.
     * @return A String containing the complete, executable JavaScript SDK.
     */
    public String generateSdkForTransformation(Transformation transformation) {
        if (transformation.targetSystemApiRequestConfigurations == null || transformation.targetSystemApiRequestConfigurations.isEmpty()) {
            return "// No Target ARCs configured for this transformation.\nvar targets = {};";
        }
        StringBuilder sdkScript = new StringBuilder();
        sdkScript.append("/**\n * Auto-generated Stay-in-Sync SDK\n * Transformation ID: ").append(transformation.id).append("\n */\n");
        sdkScript.append("var targets = {};\n\n");
        Map<Long, OpenAPI> parsedSpecs = transformation.targetSystemApiRequestConfigurations.stream()
                .map(arc -> arc.targetSystem)
                .distinct()
                .collect(Collectors.toMap(
                        system -> system.id,
                        system -> dtsGeneratorService.parseSpecification(system.openApiSpec),
                        (existing, replacement) -> existing
                ));
        for (TargetSystemApiRequestConfiguration arc : transformation.targetSystemApiRequestConfigurations) {
            OpenAPI specification = parsedSpecs.get(arc.targetSystem.id);
            sdkScript.append(generateSdkForArc(arc, specification));
        }
        return sdkScript.toString();
    }

    /**
     * Generates the JavaScript SDK snippet for a single REST Target ARC.
     * The generated code is wrapped in an IIFE (Immediately Invoked Function Expression)
     * to create a private scope, preventing variable collisions between different ARCs.
     * <p>
     * Inside this scope, it defines a JavaScript `UpsertBuilder` class that manages the construction
     * of a directive object. It then attaches a `defineUpsert` method to the global `targets` object,
     * which serves as a factory for this builder.
     *
     * @param arc           The Target ARC configuration for which to generate the SDK.
     * @param specification The parsed OpenAPI specification for the ARC's target system.
     * @return A String containing the complete JavaScript code block for one ARC.
     */
    private String generateSdkForArc(TargetSystemApiRequestConfiguration arc, OpenAPI specification) {
        String arcAlias = arc.alias;
        StringBuilder arcSdk = new StringBuilder();
        // IIFE (Immediately Invoked Function Expression) to isolate arc scopes individually
        arcSdk.append("(function() {\n");
        arcSdk.append("  'use strict';\n\n");
        arcSdk.append(String.format("  const arcAlias = '%s';\n\n", arcAlias));

        // Helper function to build path string for UPDATE
        arcSdk.append("  function buildPathStringFromProxy(path) {\n");
        arcSdk.append("    if (!path || path.length === 0) return '';\n");
        arcSdk.append("    let result = path[0];\n");
        arcSdk.append("    for (let i = 1; i < path.length; i++) {\n");
        arcSdk.append("      const segment = path[i];\n");
        arcSdk.append("      if (!isNaN(parseInt(segment, 10))) {\n");
        arcSdk.append("        result += '[' + segment + ']';\n");
        arcSdk.append("      } else {\n");
        arcSdk.append("        result += '.' + segment;\n");
        arcSdk.append("      }\n");
        arcSdk.append("    }\n");
        arcSdk.append("    return result;\n");
        arcSdk.append("  }\n\n");

        // UpsertBuilder class segment
        arcSdk.append("  class UpsertBuilder {\n");
        arcSdk.append("    constructor() {\n");
        arcSdk.append("      this.directive = {\n");
        arcSdk.append("        __directiveType: arcAlias + '_UpsertDirective',\n");
        arcSdk.append("        checkConfiguration: { parameters: {}, pathParameters: {}, payload: null },\n");
        arcSdk.append("        createConfiguration: { parameters: {}, pathParameters: {}, payload: null },\n");
        arcSdk.append("        updateConfiguration: { parameters: {}, pathParameters: {}, payload: null }\n");
        arcSdk.append("      };\n");
        arcSdk.append("    }\n\n");
        // main builder methods (usingCheck, usingCreate, usingUpdate)
        for (TargetSystemApiRequestConfigurationAction action : arc.actions) {
            arcSdk.append(generateActionWrapperMethod(action, specification));
        }
        arcSdk.append("    build() {\n");
        arcSdk.append("      return this.directive;\n");
        arcSdk.append("    }\n");
        arcSdk.append("  }\n\n"); // End of UpsertBuilder class
        arcSdk.append(String.format("  targets['%s'] = {\n", arcAlias));
        arcSdk.append("    defineUpsert: function () {\n");
        arcSdk.append("      return new UpsertBuilder();\n");
        arcSdk.append("    }\n");
        arcSdk.append("  }\n");
        arcSdk.append("})();\n\n"); // End of IIFE
        return arcSdk.toString();
    }

    /**
     * Generates one of the main builder methods: `usingCheck`, `usingCreate`, or `usingUpdate`.
     * This method accepts a `configurator` function from the user. Inside, it constructs a
     * temporary, action-specific builder object that exposes methods for setting parameters
     * (e.g., `withQueryParamId`, `withPayload`). The user's `configurator` function is then
     * executed with this temporary builder, providing a clean, chainable API.
     *
     * @param action        The configuration for the specific action (CHECK, CREATE, or UPDATE).
     * @param specification The parsed OpenAPI specification containing endpoint details.
     * @return A String containing the complete JavaScript method definition (e.g., `usingCheck(configurator) { ... }`).
     */
    private String generateActionWrapperMethod(TargetSystemApiRequestConfigurationAction action, OpenAPI specification) {
        String actionNamePascal = toPascalCase(action.actionRole.name());
        String configKey = action.actionRole.name().toLowerCase() + "Configuration";

        List<String> methodSnippets = new ArrayList<>();
        methodSnippets.addAll(generateParameterMethodSnippets(action, specification, configKey));
        generateWithPayloadMethodSnippet(action, specification, configKey).ifPresent(methodSnippets::add);

        StringBuilder method = new StringBuilder();
        method.append(String.format("    using%s(configurator) {\n", actionNamePascal));
        method.append("      const self = this;\n");
        method.append("      const builder = {\n");

        method.append(String.join(",\n", methodSnippets));

        if (!methodSnippets.isEmpty()) {
            method.append("\n");
        }

        method.append("      };\n"); // End of temporary builder object
        method.append("      if (typeof configurator === 'function') {\n");
        method.append("        configurator(builder);\n");
        method.append("      }\n");
        method.append("      return self;\n"); // Return the main builder for chaining
        method.append("    }\n\n");
        return method.toString();
    }

    /**
     * Generates the `withPayload` method snippet for an action's temporary builder object.
     * This method is only generated if the corresponding OpenAPI operation defines a request body.
     * The generated JavaScript function assigns the user-provided payload object to the
     * correct configuration key within the main directive object.
     *
     * @param action        The action being configured.
     * @param specification The parsed OpenAPI specification.
     * @param configKey     The key in the directive object where the payload should be stored (e.g., "createConfiguration").
     * @return An {@link Optional} containing the JavaScript method snippet string, or an empty Optional if no request body exists.
     */
    private Optional<String> generateWithPayloadMethodSnippet(TargetSystemApiRequestConfigurationAction action, OpenAPI specification, String configKey) {
        Operation operation = findOperation(action.endpoint, specification);

        if (operation == null || operation.getRequestBody() == null) {
            return Optional.empty();
        }

        String snippet = String.format(
                "        withPayload: function(payloadObject) {\n" +
                        "          self.directive.%s.payload = payloadObject;\n" +
                        "          return this;\n" +
                        "        }",
                configKey
        );
        return Optional.of(snippet);
    }

    /**
     * Generates a list of all parameter-setting methods (e.g., `withQueryParamId`, `withPathParamUserId`)
     * for an action's temporary builder object. It iterates through all parameters defined in the
     * OpenAPI operation for the endpoint.
     * <p>
     * It includes special, sophisticated logic for path parameters in an `UPDATE` action. It generates
     * JavaScript code that uses a {@code Proxy} to intercept property access when the user provides a
     * function (e.g., {@code res => res.body.id}). This allows the SDK to dynamically construct a placeholder
     * string like {@code {{checkResponse.body.id}}} that can be resolved at runtime.
     *
     * @param action        The action for which to generate parameter methods.
     * @param specification The parsed OpenAPI specification.
     * @param configKey     The key in the directive object where parameter values should be stored.
     * @return A List of Strings, where each string is a complete JavaScript method definition.
     */
    private List<String> generateParameterMethodSnippets(TargetSystemApiRequestConfigurationAction action, OpenAPI specification, String configKey) {
        List<String> methodSnippets = new ArrayList<>();
        Operation operation = findOperation(action.endpoint, specification);
        if (operation == null || operation.getParameters() == null) {
            return methodSnippets;
        }

        for (Parameter parameter : operation.getParameters()) {
            StringBuilder singleMethod = new StringBuilder();
            String paramName = parameter.getName();
            String paramIn = parameter.getIn();
            String methodName = "with" + toPascalCase(paramIn) + "Param" + toPascalCase(paramName);

            singleMethod.append(String.format("        %s: function(value) {\n", methodName));

            if ("path".equalsIgnoreCase(paramIn) && action.actionRole == TargetApiRequestConfigurationActionRole.UPDATE) {
                String targetKey = "pathParameters";
                singleMethod.append("          if (typeof value === 'function') {\n")
                        .append("            const path = [];\n")
                        .append("            const handler = {\n")
                        .append("              get: function(target, prop, receiver) {\n")
                        .append("                path.push(prop);\n")
                        .append("                return new Proxy({}, handler);\n")
                        .append("              }\n")
                        .append("            };\n")
                        .append("            const pathTracker = new Proxy({}, handler);\n")
                        .append("            value(pathTracker);\n")
                        .append("            const pathString = buildPathStringFromProxy(path);\n")
                        .append(String.format(
                                "            self.directive.%s.%s['%s'] = `{{checkResponse.body.${pathString}}}`;\n",
                                configKey, targetKey, paramName))
                        .append("          } else {\n")
                        .append(String.format(
                                "            self.directive.%s.%s['%s'] = value;\n",
                                configKey, targetKey, paramName))
                        .append("          }\n");
            } else {
                String targetKey = "path".equalsIgnoreCase(paramIn) ? "pathParameters" : "parameters";
                if ("path".equalsIgnoreCase(paramIn)) {
                    singleMethod.append(String.format("          self.directive.%s.%s['%s'] = value;\n", configKey, targetKey, paramName));
                } else {
                    singleMethod.append(String.format("          self.directive.%s.%s = self.directive.%s.%s || {};\n", configKey, targetKey, configKey, targetKey));
                    singleMethod.append(String.format("          self.directive.%s.%s.%s = self.directive.%s.%s.%s || {};\n", configKey, targetKey, paramIn, configKey, targetKey, paramIn));
                    singleMethod.append(String.format("          self.directive.%s.%s.%s['%s'] = value;\n", configKey, targetKey, paramIn, paramName));
                }
            }
            singleMethod.append("          return this;\n");
            singleMethod.append("        }");
            methodSnippets.add(singleMethod.toString());
        }
        return methodSnippets;
    }

    /**
     * A utility method to find the corresponding OpenAPI {@link Operation} object within a parsed specification
     * based on an endpoint's path and HTTP method.
     *
     * @param endpoint      The {@link SyncSystemEndpoint} to find.
     * @param specification The {@link OpenAPI} specification to search within.
     * @return The found {@link Operation} object, or {@code null} if not found.
     */
    private Operation findOperation(SyncSystemEndpoint endpoint, OpenAPI specification) {
        if (specification == null || specification.getPaths() == null) return null;
        PathItem pathItem = specification.getPaths().get(endpoint.endpointPath);
        if (pathItem == null) return null;
        return switch (endpoint.httpRequestType.toUpperCase()) {
            case "GET" -> pathItem.getGet();
            case "POST" -> pathItem.getPost();
            case "PUT" -> pathItem.getPut();

            default -> null;
        };
    }

    /**
     * A utility function to convert a string from any case (snake_case, kebab-case, space-separated)
     * to a format suitable for method or class names (typically a variation of PascalCase).
     *
     * @param s The input string.
     * @return The formatted version of the string.
     */
    private String toPascalCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return Arrays.stream(s.split("[_\\- ]"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining());
    }
}