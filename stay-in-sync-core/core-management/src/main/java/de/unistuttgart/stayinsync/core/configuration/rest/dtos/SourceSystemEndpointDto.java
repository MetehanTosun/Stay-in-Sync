package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.List;

@Schema(name = "SourceSystemEndpoint", description = "DTO for a source system endpoint")
public record SourceSystemEndpointDto(
    @Schema(description = "Unique identifier", example = "123")
    Long id,

    @Schema(description = "Path of the endpoint", example = "/api/data")
    String endpointPath,

    @Schema(description = "HTTP method", example = "GET")
    String httpRequestType,

    @Schema(description = "Polling active flag")
    boolean pollingActive,

    @Schema(description = "Polling interval in milliseconds", example = "60000")
    Integer pollingRateInMs,

    @Schema(description = "Generated JSON schema for this endpoint")
    String jsonSchema,

    @Schema(description = "Mode of the schema: auto/manual", example = "auto")
    String schemaMode,

    @Schema(description = "List of request headers for the endpoint")
    List<ApiRequestHeaderDto> apiRequestHeaders,

    @Schema(description = "List of query parameters for the endpoint")
    List<ApiQueryParamDto> apiQueryParams,

    @Schema(description = "List of path variables for the endpoint")
    List<SourceSystemVariableDto> sourceSystemVariables
) {}