package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiEndpointQueryParamMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamDTO;
import de.unistuttgart.stayinsync.core.configuration.service.ApiEndpointQueryParamService;
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

@Path("/api/config/endpoint/")
public class QueryParamResource {

    @Inject
    ApiEndpointQueryParamService apiEndpointQueryParamService;

    @Inject
    ApiEndpointQueryParamMapper fullUpdateMapper;

    @Path("/endpoint/{endpointId}/query-param")
    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a valid query-param for the specified endpoint")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created query-param",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid query-param passed in (or no request body found)"
    )
    public Response createEndpointQueryParam(
            @RequestBody(
                    name = "query-param",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = ApiEndpointQueryParamDTO.class),
                            examples = @ExampleObject(name = "valid_endpoint_param", value = Examples.VALID_ENDPOINT_PARAM_POST)
                    )
            )
            @PathParam("endpointId") Long endpointId,
            @Valid @NotNull ApiEndpointQueryParamDTO apiEndpointQueryParamDTO,
            @Context UriInfo uriInfo) {

        var persistedEndpointQueryParam = this.apiEndpointQueryParamService.persistApiQueryParam(apiEndpointQueryParamDTO, endpointId);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedEndpointQueryParam.id));
        Log.debugf("New query-param created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }

    @GET
    @Operation(summary = "Returns all the query-params for the specified endpoint id from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all query params",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = ApiEndpointQueryParamDTO.class, type = SchemaType.ARRAY)
            )
    )
    @Path("/endpoint/{endpointId}/query-param")
    public List<ApiEndpointQueryParamDTO> getAllQueryParamsForEndpoint(@Parameter(name = "endpoint", description = "Id of the associated endpoint") @PathParam("endpointId") Long endpointId) {
        var allQueryParamsByEndpointId = this.apiEndpointQueryParamService.findAllQueryParamsByEndpointId(endpointId);

        Log.debugf("Total number of api-query-params by id: %d", allQueryParamsByEndpointId.size());

        return fullUpdateMapper.mapToDTOList(allQueryParamsByEndpointId);
    }


    @GET
    @Path("/endpoint/query-param/{id}")
    @Operation(summary = "Returns a query-param for a given identifier")
    @APIResponse(
            responseCode = "200",
            description = "Gets a query-param for a given id",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = ApiEndpointQueryParamDTO.class),
                    examples = @ExampleObject(name = "query-param", value = Examples.VALID_EXAMPLE_SYNCJOB)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The query-param is not found for a given identifier"
    )
    public Response getQueryParamById(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.apiEndpointQueryParamService.findQueryParamById(id)
                .map(apiEndpointQueryParam -> {
                    Log.debugf("Found query-param: %s", apiEndpointQueryParam);
                    return Response.ok(fullUpdateMapper.mapToDTO(apiEndpointQueryParam)).build();
                })
                .orElseThrow(() -> {
                    Log.warnf("No query-param found using id %d", id);
                    return new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find query-param", "No query-param found using id %d", id);
                });
    }

    @DELETE
    @Operation(summary = "Deletes an exiting query-param")
    @APIResponse(
            responseCode = "204",
            description = "Delete a query-param"
    )
    @Path("/endpoint/query-param/{id}")
    public void deleteEndpointQueryParam(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.apiEndpointQueryParamService.deleteQueryParamById(id);
        Log.debugf("query-param with id %d deleted ", id);
    }

    @PUT
    @Path("/endpoint/query-param/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates an exiting query-param by replacing it with the passed-in query-param")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the query-param"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid query-param passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No query-param found"
    )
    public Response fullyUpdateEndpointQueryParam(@Parameter(name = "id", required = true)
                                                  @RequestBody(
                                                          name = "query-param",
                                                          required = true,
                                                          content = @Content(
                                                                  mediaType = APPLICATION_JSON,
                                                                  schema = @Schema(implementation = ApiEndpointQueryParamDTO.class),
                                                                  examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_SYNCJOB)
                                                          )
                                                  )
                                                  @PathParam("id") Long id, @Valid @NotNull ApiEndpointQueryParamDTO apiEndpointQueryParamDTO) {
        if (id != apiEndpointQueryParamDTO.id()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Id missmatch", "Make sure that the request body entity id matches the request parameter");
        }

        return this.apiEndpointQueryParamService.replaceQueryParamById(apiEndpointQueryParamDTO)
                .map(updatedEndpointQueryParam -> {
                    Log.debugf("query-param replaced with new values %s", updatedEndpointQueryParam);
                    return Response.noContent().build();
                })
                .orElseGet(() -> {
                    Log.debugf("No query-param found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }
}
