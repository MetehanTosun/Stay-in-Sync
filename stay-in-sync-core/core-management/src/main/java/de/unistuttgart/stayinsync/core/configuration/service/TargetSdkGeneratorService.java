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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates the executable JavaScript SDK for a given Transformation.
 * This SDK creates the 'targets' object in the GraalJS context, providing a fluent API
 * for building directives based on the configured Target ARCs.
 * The generated script is intended to be persisted with the Transformation entity.
 */
@ApplicationScoped
public class TargetSdkGeneratorService {

    @Inject
    TargetDtsBuilderGeneratorService dtsGeneratorService;

    /**
     * Generates the full JavaScript SDK script for a given Transformation entity.
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
     * Generates the JavaScript SDK snippet for a single Target ARC.
     */
    private String generateSdkForArc(TargetSystemApiRequestConfiguration arc, OpenAPI specification) {
        String arcAlias = arc.alias;
        StringBuilder arcSdk = new StringBuilder();

        // IIFE (Immediately Invoked Function Expression) to isolate arc scopes individually
        arcSdk.append("(function() {\n");
        arcSdk.append("  'use strict';\n\n");
        arcSdk.append(String.format("  const arcAlias = '%s';\n\n", arcAlias));
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
     * Generates the main `usingCheck`, `usingCreate`, or `usingUpdate` method.
     * This method creates a temporary builder object for configuring the specific action.
     */
    private String generateActionWrapperMethod(TargetSystemApiRequestConfigurationAction action, OpenAPI specification) {
        String actionNamePascal = toPascalCase(action.actionRole.name());
        String configKey = action.actionRole.name().toLowerCase() + "Configuration";

        StringBuilder method = new StringBuilder();
        method.append(String.format("    using%s(configurator) {\n", actionNamePascal));
        method.append("      const self = this;\n");  // Reference to the main UpsertBuilder instance
        method.append("      const builder = {\n");

        // Generate all parameter methods (e.g., withQueryParamSku)
        method.append(generateParameterMethods(action, specification, configKey));

        // Generate the withPayload method if applicable
        method.append(generateWithPayloadMethod(action, specification, configKey));

        method.append("      };\n"); // End of temporary builder object
        method.append("      if (typeof configurator === 'function') {\n");
        method.append("        configurator(builder);\n");
        method.append("      }\n");
        method.append("      return self;\n"); // Return the main builder for chaining
        method.append("    }\n\n");
        return method.toString();
    }

    /**
     * Generates the `withPayload` method for the temporary action builder.
     */
    private String generateWithPayloadMethod(TargetSystemApiRequestConfigurationAction action, OpenAPI specification, String configKey) {
        Operation operation = findOperation(action.endpoint, specification);

        if (operation == null || operation.getRequestBody() == null) {
            return "";
        }

        return String.format(
                "        withPayload: function(payloadObject) {\n" +
                        "          self.directive.%s.payload = payloadObject;\n" +
                        "          return this;\n" +
                        "        },\n",
                configKey
        );
    }

    private String generateParameterMethods(TargetSystemApiRequestConfigurationAction action, OpenAPI specification, String configKey) {
        StringBuilder methods = new StringBuilder();
        Operation operation = findOperation(action.endpoint, specification);
        if (operation == null || operation.getParameters() == null) {
            return "";
        }

        for (Parameter parameter : operation.getParameters()) {
            String paramName = parameter.getName();
            String paramIn = parameter.getIn();

            String methodName = "with" + toPascalCase(paramIn) + "Param" + toPascalCase(paramName);
            String targetKey = paramIn.equalsIgnoreCase("path") ? "pathParameters" : "parameters";

            methods.append(String.format("        %s: function(value) {\n", methodName));

            if (action.actionRole == TargetApiRequestConfigurationActionRole.UPDATE && "path".equals(paramIn)) {
                methods.append("          if (typeof value === 'function') {\n")
                        .append(
                        // The function itself is just a marker. We generate a standardized placeholder.
                        // The name of the parameter in the lambda (e.g., `checkResponse`) is irrelevant here.
                        // The placeholder path is constructed based on the parameter name from the OpenAPI spec.
                        String.format("            self.directive.%s.%s['%s'] = '{{checkResponse.body.%s}}';\n", configKey, targetKey, paramName, paramName))
                        .append("          } else {\n")
                        .append(
                        // Static value was provided
                        String.format("            self.directive.%s.%s['%s'] = value;\n", configKey, targetKey, paramName))
                        .append("          }\n");
            } else {
                // Standard case for all other parameters
                methods.append(String.format(
                        "          self.directive.%s.%s = self.directive.%s.%s || {};\n",
                        configKey, targetKey, configKey, targetKey
                ));
                if (!"path".equalsIgnoreCase(paramIn)) { // Query and Header params are nested
                    methods.append(String.format(
                            "          self.directive.%s.%s.%s = self.directive.%s.%s.%s || {};\n",
                            configKey, targetKey, paramIn, configKey, targetKey, paramIn
                    ));
                    methods.append(String.format(
                            "          self.directive.%s.%s.%s['%s'] = value;\n",
                            configKey, targetKey, paramIn, paramName
                    ));
                } else {
                    methods.append(String.format(
                            "          self.directive.%s.%s['%s'] = value;\n",
                            configKey, targetKey, paramName
                    ));
                }
            }
            methods.append("          return this;\n");
            methods.append("        },\n");
        }

        return methods.toString();
    }

    private Operation findOperation(SyncSystemEndpoint endpoint, OpenAPI specification) {
        if (specification == null || specification.getPaths() == null) return null;
        PathItem pathItem = specification.getPaths().get(endpoint.endpointPath);
        if (pathItem == null) return null;
        return switch (endpoint.httpRequestType.toUpperCase()) {
            case "GET" -> pathItem.getGet();
            case "POST" -> pathItem.getPost();
            case "PUT" -> pathItem.getPut();
            // Add other methods as needed
            default -> null;
        };
    }

    private String toPascalCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return Arrays.stream(s.split("[_\\- ]"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining());
    }
}
