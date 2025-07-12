package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateApiHeaderDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemEndpointDTO;
import de.unistuttgart.stayinsync.transport.domain.ApiEndpointQueryParamType;
import de.unistuttgart.stayinsync.transport.domain.ApiRequestHeaderType;
import io.quarkus.logging.Log;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@ApplicationScoped
public class OpenApiSpecificationParserService {

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    ApiEndpointQueryParamService apiEndpointQueryParamService;

    @Inject
    ApiHeaderService apiHeaderService;

    @Transactional
    public void synchronizeFromSpec(SourceSystem sourceSystem) {
        if (sourceSystem.openApiSpec == null || sourceSystem.openApiSpec.length == 0) {
            Log.infof("No OpenAPI spec provided for SourceSystem ID %d. Skipping sync.", sourceSystem.id);
            return;
        }

        String specAsString = new String(sourceSystem.openApiSpec, StandardCharsets.UTF_8);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(specAsString, null, new ParseOptions());
        OpenAPI openAPI = result.getOpenAPI();

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            result.getMessages().forEach(Log::warn);
        }
        if (openAPI == null) {
            Log.errorf("Failed to parse OpenAPI specification for SourceSystem ID %id.", sourceSystem.id);
            return;
        }

        // TODO: refresh db model with current openAPI spec (reconciliation, deletion and update of old spec
        // TODO: keep old values Set for future queryParams

        processSecuritySchemes(openAPI, sourceSystem);

        processPaths(openAPI, sourceSystem);
    }

    private void processSecuritySchemes(OpenAPI openAPI, SourceSystem sourceSystem) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSecuritySchemes() == null || openAPI.getComponents().getSecuritySchemes().isEmpty()) {
            Log.debugf("No security schemes found in spec for SourceSystem ID %d.", sourceSystem.id);
            return;
        }

        for (Map.Entry<String, SecurityScheme> entry : openAPI.getComponents().getSecuritySchemes().entrySet()) {
            SecurityScheme securityScheme = entry.getValue();

            // cookie/query based schemes are skipped, we only care about schemes resulting in a request header.
            if (securityScheme.getIn() != SecurityScheme.In.HEADER && securityScheme.getType() != SecurityScheme.Type.HTTP) {
                continue;
            }

            String headerName;
            ApiRequestHeaderType headerType;

            if (securityScheme.getType() == SecurityScheme.Type.APIKEY) {
                headerName = securityScheme.getName();
                headerType = determineHeaderTypeFromName(headerName);

            } else if (securityScheme.getType() == SecurityScheme.Type.HTTP) {
                headerName = "Authorization";
                headerType = ApiRequestHeaderType.AUTHORIZATION;

            } else {
                continue; // TODO: handle possible future types.
            }

            boolean alreadyExists = apiHeaderService.findAllHeadersBySyncSystemId(sourceSystem.id)
                    .stream()
                    .anyMatch(h -> h.headerName.equalsIgnoreCase(headerName));

            if (!alreadyExists) {
                Log.infof("Found new header definition in spec: '%s'. Creating entity for SourceSystem ID %d.", headerName, sourceSystem.id);

                CreateApiHeaderDTO headerDTO = new CreateApiHeaderDTO(headerType, headerName);

                apiHeaderService.persistRequestHeader(headerDTO, sourceSystem.id);
            } else {
                Log.debugf("Header definition for '%s' already exists for SourceSystem ID %d. Skipping creation.", headerName, sourceSystem.id);
            }
        }
    }

    private void processPaths(OpenAPI openAPI, SourceSystem sourceSystem) {
        if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            return;
        }

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathItem.readOperationsMap().entrySet()) {
                PathItem.HttpMethod httpMethod = opEntry.getKey();
                Operation operation = opEntry.getValue();

                CreateSourceSystemEndpointDTO endpointDTO = new CreateSourceSystemEndpointDTO(
                        path,
                        httpMethod.toString()
                        // operation.getSummary() != null ? operation.getSummary() : operation.getDescription() // TODO: summary for endpoint?
                );
                SourceSystemEndpoint newEndpoint = sourceSystemEndpointService.persistSourceSystemEndpoint(endpointDTO, sourceSystem.id);

                if (operation.getParameters() != null) {
                    for (Parameter parameter : operation.getParameters()) {
                        processParameter(parameter, newEndpoint);
                    }
                }
            }
        }
    }

    private void processParameter(Parameter parameter, SourceSystemEndpoint endpoint) {
        ApiEndpointQueryParamType paramType;
        switch (parameter.getIn()) {
            case "query":
                paramType = ApiEndpointQueryParamType.QUERY;
                break;
            case "path":
                paramType = ApiEndpointQueryParamType.PATH;
                break;
            default:
                return;
        }

        ApiEndpointQueryParamDTO paramDTO = new ApiEndpointQueryParamDTO(
                parameter.getName(),
                paramType,
                null, // TODO: not sure, which values this is
                null
        );
        apiEndpointQueryParamService.persistApiQueryParam(paramDTO, endpoint.id);
    }

    /**
     * A helper method to map a header name string to your specific enum type.
     * This provides some intelligence to the parsing process.
     *
     * @param headerName The header name from the OpenAPI spec.
     * @return The corresponding ApiRequestHeaderType enum.
     */
    private ApiRequestHeaderType determineHeaderTypeFromName(String headerName) {
        if (headerName == null) {
            return ApiRequestHeaderType.CUSTOM;
        }
        return switch (headerName.toLowerCase()) {
            case "authorization" -> ApiRequestHeaderType.AUTHORIZATION;
            case "content-type" -> ApiRequestHeaderType.CONTENT_TYPE;
            case "accept" -> ApiRequestHeaderType.ACCEPT;
            case "user-agent" -> ApiRequestHeaderType.USER_AGENT;
            case "cache-control" -> ApiRequestHeaderType.CACHE_CONTROL;
            case "accept-charset" -> ApiRequestHeaderType.ACCEPT_CHARSET;
            default -> ApiRequestHeaderType.CUSTOM;
        };
    }
}
