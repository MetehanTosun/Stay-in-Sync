package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiHeaderValueMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiHeaderValueDTO;
import de.unistuttgart.stayinsync.core.configuration.service.ApiHeaderValueService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.net.URI;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/request-configuration/")
public class ApiHeaderValueResource {

    @Inject
    ApiHeaderValueService apiHeaderValueService;

    @Inject
    ApiHeaderValueMapper fullUpdateMapper;

    @Path("/{requestConfigId}/request-header")
    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a valid api-request-header for the specified source system")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created api-request-header",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid api-request-header passed in (or no request body found)"
    )
    public Response createApiRequestHeader(
            @RequestBody(
                    name = "api-request-header",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = ApiHeaderValueDTO.class),
                            examples = @ExampleObject(name = "valid_source_api_request_header", value = Examples.VALID_API_HEADER_POST)
                    )
            )
            @PathParam("requestConfigId") Long sourceSystemId,
            @Valid @NotNull ApiHeaderValueDTO ApiHeaderValueDTO,
            @Context UriInfo uriInfo) {

        var persistedHeaderValue = this.apiHeaderValueService.persistHeaderValue(ApiHeaderValueDTO, sourceSystemId);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedHeaderValue.id));
        Log.infof("New api-request-header-value created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }

    @GET
    @Operation(summary = "Returns all the source-system-endpoints for the specified source-system from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all source-system-endpoints",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = ApiHeaderValueDTO.class, type = SchemaType.ARRAY)
            )
    )
    @Path("/{requestConfigId}/request-header")
    public List<ApiHeaderValueDTO> getAllApiHeaderValues(@Parameter(name = "source_system_filter", description = "An optional filter parameter to filter results by source system id") @PathParam("requestConfigId") Long requestConfigId) {
        var apiRequestHeaderValues = this.apiHeaderValueService.findByRequestConfigurationId(requestConfigId);

        Log.infof("Total number of request headers per configuration: %d", apiRequestHeaderValues.size());

        return fullUpdateMapper.mapToDTOList(apiRequestHeaderValues);
    }


    @GET
    @Path("/request-header/{id}")
    @Operation(summary = "Returns a api-request-header-value for a given identifier")
    @APIResponse(
            responseCode = "200",
            description = "Gets a api-request-header-value for a given id",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = ApiHeaderValueDTO.class),
                    examples = @ExampleObject(name = "api-request-header-value", value = Examples.VALID_EXAMPLE_SYNCJOB)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The api-request-header-value is not found for a given identifier"
    )
    public Response getApiRequestHeaderById(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.apiHeaderValueService.findHeaderValueById(id)
                .map(apiRequestHeader -> {
                    Log.infof("Found api-request-header-value: %s", apiRequestHeader);
                    return Response.ok(fullUpdateMapper.mapToDTO(apiRequestHeader)).build();
                })
                .orElseThrow(() -> {
                    Log.warnf("No api-request-header-value found using id %d", id);
                    return new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find api-request-header-value", "No api-request-header-value found using id %d", id);
                });
    }

    @DELETE
    @Operation(summary = "Deletes an exiting api-request-header-value")
    @APIResponse(
            responseCode = "204",
            description = "Delete a api-request-header-value"
    )
    @Path("/request-header/{id}")
    public void deleteApiHeaderValue(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.apiHeaderValueService.deleteApiHeaderValueById(id);
        Log.infof("api-request-header-value with id %d deleted ", id);
    }

    @PUT
    @Path("/request-header/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates an exiting api-request-header-value by replacing it with the passed-in api-request-header-value")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the api-request-header-value"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid api-request-header-value passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No api-request-header-value found"
    )
    public Response fullyUpdateApiHeaderValue(@Parameter(name = "id", required = true)
                                              @RequestBody(
                                                      name = "api-request-header-value",
                                                      required = true,
                                                      content = @Content(
                                                              mediaType = APPLICATION_JSON,
                                                              schema = @Schema(implementation = ApiHeaderValueDTO.class),
                                                              examples = @ExampleObject(name = "valid_api_header", value = Examples.VALID_API_HEADER_VALUE)
                                                      )
                                              )
                                              @PathParam("id") Long id, @Valid @NotNull ApiHeaderValueDTO apiHeaderValueDTO) {

        return this.apiHeaderValueService.replaceHeaderValue(apiHeaderValueDTO)
                .map(updatedSourceSystemEndpoint -> {
                    Log.infof("api-request-header-value replaced with new values %s", updatedSourceSystemEndpoint);
                    return Response.noContent().build();
                })
                .orElseGet(() -> {
                    Log.infof("No api-request-header-value found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }
}
