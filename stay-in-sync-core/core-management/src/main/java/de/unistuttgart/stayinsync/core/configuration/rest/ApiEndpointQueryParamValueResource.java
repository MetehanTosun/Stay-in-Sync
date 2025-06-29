package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiHeaderFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiRequestHeaderDTO;
import de.unistuttgart.stayinsync.core.configuration.service.ApiRequestHeaderService;
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

@Path("/api/config/query-param/")
public class ApiEndpointQueryParamValueResource {

    @Inject
    ApiRequestHeaderService apiRequestHeaderService;

    @Inject
    ApiHeaderFullUpdateMapper fullUpdateMapper;

    @Path("/{paramId}/value")
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
                            schema = @Schema(implementation = ApiRequestHeaderDTO.class),
                            examples = @ExampleObject(name = "valid_source_api_request_header", value = Examples.VALID_API_REQUEST_HEADER_POST)
                    )
            )
            @PathParam("paramId") Long paramId,
            @Valid @NotNull ApiRequestHeaderDTO apiRequestHeaderDTO,
            @Context UriInfo uriInfo) {

        var persistedSourceSystemEndpoint = this.apiRequestHeaderService.persistRequestHeader(apiRequestHeaderDTO, paramId);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedSourceSystemEndpoint.id));
        Log.debugf("New api-request-header created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }

    @GET
    @Operation(summary = "Returns all the source-system-endpoints for the specified source-system from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all source-system-endpoints",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = ApiRequestHeaderDTO.class, type = SchemaType.ARRAY)
            )
    )
    @Path("/{paramId}/value")
    public List<ApiRequestHeaderDTO> getAllSourceSystemEndpoints(@Parameter(name = "source_system_filter", description = "An optional filter parameter to filter results by source system id") @PathParam("paramId") Long paramId) {
        var apiRequestHeaders = this.apiRequestHeaderService.findAllHeadersBySyncSystemId(paramId);

        Log.debugf("Total number of source-system-endpoints: %d", apiRequestHeaders.size());

        return fullUpdateMapper.mapToDTOList(apiRequestHeaders);
    }


    @GET
    @Path("/request-header/{id}")
    @Operation(summary = "Returns a api-request-header for a given identifier")
    @APIResponse(
            responseCode = "200",
            description = "Gets a api-request-header for a given id",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = ApiRequestHeaderDTO.class),
                    examples = @ExampleObject(name = "api-request-header", value = Examples.VALID_EXAMPLE_SYNCJOB)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The api-request-header is not found for a given identifier"
    )
    public Response getApiRequestHeaderById(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.apiRequestHeaderService.findRequestHeaderById(id)
                .map(apiRequestHeader -> {
                    Log.debugf("Found api-request-header: %s", apiRequestHeader);
                    return Response.ok(fullUpdateMapper.mapToDTO(apiRequestHeader)).build();
                })
                .orElseThrow(() -> {
                    Log.warnf("No api-request-header found using id %d", id);
                    return new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find api-request-header", "No api-request-header found using id %d", id);
                });
    }

    @DELETE
    @Operation(summary = "Deletes an exiting api-request-header")
    @APIResponse(
            responseCode = "204",
            description = "Delete a api-request-header"
    )
    @Path("/request-header/{id}")
    public void deleteSourceSystemEndpoint(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.apiRequestHeaderService.deleteRequestHeaderById(id);
        Log.debugf("api-request-header with id %d deleted ", id);
    }

    @PUT
    @Path("/request-header/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates an exiting api-request-header by replacing it with the passed-in api-request-header")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the api-request-header"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid api-request-header passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No api-request-header found"
    )
    public Response fullyUpdateSourceSystemEndpoint(@Parameter(name = "id", required = true)
                                                    @RequestBody(
                                                            name = "api-request-header",
                                                            required = true,
                                                            content = @Content(
                                                                    mediaType = APPLICATION_JSON,
                                                                    schema = @Schema(implementation = ApiRequestHeaderDTO.class),
                                                                    examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_SYNCJOB)
                                                            )
                                                    )
                                                    @PathParam("id") Long id, @Valid @NotNull ApiRequestHeaderDTO apiRequestHeaderDTO) {
        if (id != apiRequestHeaderDTO.id()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Id missmatch", "Make sure that the request body entity id matches the request parameter");
        }

        return this.apiRequestHeaderService.replaceRequestHeader(apiRequestHeaderDTO)
                .map(updatedSourceSystemEndpoint -> {
                    Log.debugf("api-request-header replaced with new values %s", updatedSourceSystemEndpoint);
                    return Response.noContent().build();
                })
                .orElseGet(() -> {
                    Log.debugf("No api-request-header found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }
}
