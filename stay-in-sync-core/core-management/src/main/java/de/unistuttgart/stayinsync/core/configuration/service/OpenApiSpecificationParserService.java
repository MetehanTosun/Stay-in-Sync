package de.unistuttgart.stayinsync.core.configuration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamDTO;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateTargetSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateApiHeaderDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemEndpointDTO;
import de.unistuttgart.stayinsync.transport.domain.ApiEndpointQueryParamType;
import de.unistuttgart.stayinsync.transport.domain.ApiRequestHeaderType;
import de.unistuttgart.stayinsync.transport.dto.SchemaType;
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

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class OpenApiSpecificationParserService {

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    TargetSystemEndpointService targetSystemEndpointService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ApiEndpointQueryParamService apiEndpointQueryParamService;

    @Inject
    ApiHeaderService apiHeaderService;

    @Transactional
    public void synchronizeFromSpec(SourceSystem sourceSystem) {
        if (sourceSystem.openApiSpec == null || sourceSystem.openApiSpec.isBlank()) {
            Log.infof("No OpenAPI spec provided for SourceSystem ID %d. Skipping sync.", sourceSystem.id);
            return;
        }
    
        try {
            String specContent;
            
           
            if (sourceSystem.openApiSpec.startsWith("http")) {
                Log.infof("📥 Downloading OpenAPI spec from URL: %s", sourceSystem.openApiSpec);
                specContent = downloadFromUrl(sourceSystem.openApiSpec);
            } else {
                Log.infof("📄 Using provided OpenAPI spec content directly");
                specContent = sourceSystem.openApiSpec; 
            }
    
            ParseOptions parseOptions = new ParseOptions();
            parseOptions.setResolve(true); // Enable reference resolution
            parseOptions.setResolveFully(true); // Fully resolve all references
            SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent, null, parseOptions);
            OpenAPI openAPI = result.getOpenAPI();
    
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                result.getMessages().forEach(Log::warn);
            }
            
            if (openAPI == null) {
                Log.errorf("Failed to parse OpenAPI specification for SourceSystem ID %d.", sourceSystem.id);
                return;
            }
    
            // TODO: refresh db model with current openAPI spec (reconciliation, deletion and update of old spec
            // TODO: keep old values Set for future queryParams
    
            processSecuritySchemes(openAPI, sourceSystem);
            processPaths(openAPI, sourceSystem);
            
        } catch (Exception e) {
            Log.warnf("Failed to process OpenAPI specification for SourceSystem ID %d: %s", sourceSystem.id, e.getMessage());
        }
    }

    @Transactional
    public void synchronizeFromSpec(TargetSystem targetSystem) {
        if (targetSystem.openApiSpec == null || targetSystem.openApiSpec.isBlank()) {
            Log.infof("No OpenAPI spec provided for TargetSystem ID %d. Skipping sync.", targetSystem.id);
            return;
        }

        try {
            String specContent;

            if (targetSystem.openApiSpec.startsWith("http")) {
                Log.infof("📥 Downloading OpenAPI spec from URL: %s", targetSystem.openApiSpec);
                specContent = downloadFromUrl(targetSystem.openApiSpec);
            } else {
                Log.infof("📄 Using provided OpenAPI spec content directly");
                specContent = targetSystem.openApiSpec;
            }

            ParseOptions parseOptions = new ParseOptions();
            parseOptions.setResolve(true); // Enable reference resolution
            parseOptions.setResolveFully(true); // Fully resolve all references
            SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent, null, parseOptions);
            OpenAPI openAPI = result.getOpenAPI();

            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                result.getMessages().forEach(Log::warn);
            }

            if (openAPI == null) {
                Log.errorf("Failed to parse OpenAPI specification for TargetSystem ID %d.", targetSystem.id);
                return;
            }

            processPathsForTarget(openAPI, targetSystem);

        } catch (Exception e) {
            Log.warnf("Failed to process OpenAPI specification for TargetSystem ID %d: %s", targetSystem.id, e.getMessage());
        }
    }
    
    
    private String downloadFromUrl(String url) throws Exception {
        try (var client = java.net.http.HttpClient.newHttpClient()) {
            var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Accept", "application/json, application/yaml, text/yaml")
                .build();
            
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Log.infof("✅ Successfully downloaded OpenAPI spec from: %s", url);
                return response.body();
            } else {
                throw new RuntimeException("Failed to download OpenAPI spec. Status: " + response.statusCode());
            }
        }
    }
    private void processSecuritySchemes(OpenAPI openAPI, SourceSystem sourceSystem) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSecuritySchemes() == null || openAPI.getComponents().getSecuritySchemes().isEmpty()) {
            Log.debugf("No security schemes found in spec for SourceSystem ID %d.", sourceSystem.id);
            return;
        }

        for (Map.Entry<String, SecurityScheme> entry : openAPI.getComponents().getSecuritySchemes().entrySet()) {
            SecurityScheme securityScheme = entry.getValue();

            
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
                continue;
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

                String requestBodySchema = null;
                if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                    var mediaType = operation.getRequestBody().getContent().get("application/json");
                    if (mediaType != null && mediaType.getSchema() != null) {
                        try {
                            var cleanedSchema = cleanSchema(mediaType.getSchema());
                            requestBodySchema = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cleanedSchema);
                        } catch (Exception e) {
                            requestBodySchema = null;
                        }
                    }
                }

                String responseBodySchema = null;
                if (operation.getResponses() != null && operation.getResponses().get("200") != null) {
                    var response = operation.getResponses().get("200");
                    if (response.getContent() != null && response.getContent().get("application/json") != null) {
                        var mediaType = response.getContent().get("application/json");
                        if (mediaType.getSchema() != null) {
                        try {
                            var cleanedSchema = cleanSchema(mediaType.getSchema());
                            responseBodySchema = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cleanedSchema);
                        } catch (Exception e) {
                            responseBodySchema = null;
                        }
                        }
                    }
                }

                CreateSourceSystemEndpointDTO endpointDTO = new CreateSourceSystemEndpointDTO(
                        path,
                        httpMethod.toString(),
                        requestBodySchema,
                        responseBodySchema
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

    private void processPathsForTarget(OpenAPI openAPI, TargetSystem targetSystem) {
        if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            return;
        }

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathItem.readOperationsMap().entrySet()) {
                PathItem.HttpMethod httpMethod = opEntry.getKey();

                // Idempotenz: existiert bereits?
                var existing = io.quarkus.hibernate.orm.panache.Panache.getEntityManager()
                        .createQuery("SELECT t FROM TargetSystemEndpoint t WHERE t.targetSystem.id = :tsId AND t.endpointPath = :path AND t.httpRequestType = :method")
                        .setParameter("tsId", targetSystem.id)
                        .setParameter("path", path)
                        .setParameter("method", httpMethod.toString())
                        .getResultList();

                if (existing != null && !existing.isEmpty()) {
                    Log.debugf("Target endpoint already exists for %s %s on TargetSystem %d. Skipping.", httpMethod, path, targetSystem.id);
                    continue;
                }

                CreateTargetSystemEndpointDTO endpointDTO = new CreateTargetSystemEndpointDTO(
                        path,
                        httpMethod.toString(),
                        null,
                        null
                );

                targetSystemEndpointService.persistTargetSystemEndpoint(endpointDTO, targetSystem.id);
            }
        }
    }

    private void processParameter(Parameter parameter, SourceSystemEndpoint endpoint) {
        ApiEndpointQueryParamType paramType;
        SchemaType schemaType = null;
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

        switch (parameter.getSchema().getType()) {
            case "string":
                schemaType = SchemaType.STRING;
                break;
            case "integer":
                schemaType = SchemaType.INTEGER;
                break;
            case "number":
                schemaType = SchemaType.NUMBER;
                break;
            case "boolean":
                schemaType = SchemaType.BOOLEAN;
                break;
            case "array":
                schemaType = SchemaType.ARRAY;
                break;
            default:
                Log.debugf("Schema description for parameter %s for endpoint with ID %s is not defined", parameter.getName(), endpoint.id);
        }

        ApiEndpointQueryParamDTO paramDTO = new ApiEndpointQueryParamDTO(
                parameter.getName(),
                paramType,
                schemaType,
                null, 
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

    /**
     * Cleans a schema by removing null values and metadata, keeping only relevant fields
     */
    private Map<String, Object> cleanSchema(io.swagger.v3.oas.models.media.Schema<?> schema) {
        Map<String, Object> cleaned = new HashMap<>();
        
        if (schema == null) {
            return cleaned;
        }
        
        // Only include non-null, relevant fields
        if (schema.getType() != null) {
            cleaned.put("type", schema.getType());
        }
        if (schema.getDescription() != null) {
            cleaned.put("description", schema.getDescription());
        }
        if (schema.getFormat() != null) {
            cleaned.put("format", schema.getFormat());
        }
        if (schema.getExample() != null) {
            cleaned.put("example", schema.getExample());
        }
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            cleaned.put("enum", schema.getEnum());
        }
        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            cleaned.put("required", schema.getRequired());
        }
        
        // Handle properties
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            Map<String, Object> cleanedProperties = new HashMap<>();
            for (Map.Entry<String, io.swagger.v3.oas.models.media.Schema> propEntry : schema.getProperties().entrySet()) {
                cleanedProperties.put(propEntry.getKey(), cleanSchema(propEntry.getValue()));
            }
            cleaned.put("properties", cleanedProperties);
        }
        
        // Handle items (for arrays)
        if (schema.getItems() != null) {
            cleaned.put("items", cleanSchema(schema.getItems()));
        }
        
        // Handle $ref (if not resolved)
        if (schema.get$ref() != null) {
            cleaned.put("$ref", schema.get$ref());
        }
        
        return cleaned;
    }
}
