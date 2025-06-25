package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record SourceSystemEndpointDto(
    @Schema(description = "Unique identifier of the endpoint")
    Long id,

    @Schema(description = "Path of the endpoint, e.g. /users/{id}")
    String endpointPath,

    @Schema(description = "HTTP method to call (GET, POST, etc.)")
    String httpRequestType,

    @Schema(description = "Indicates if polling is active for this endpoint")
    boolean pollingActive,

    @Schema(description = "Polling rate in milliseconds")
    Integer pollingRateInMs,

    @Schema(description = "JSON schema generated or provided for this endpoint")
    String jsonSchema,

    @Schema(description = "Mode of schema creation: 'auto' or 'manual'")
    String schemaMode,

    @Schema(description = "List of request headers for the endpoint")
    List<ApiRequestHeaderDto> apiRequestHeaders,

    @Schema(description = "List of query parameters for the endpoint")
    List<ApiQueryParamDto> apiQueryParams,

    @Schema(description = "List of path variables for the endpoint")
    List<SourceSystemVariableDto> sourceSystemVariables
) {}