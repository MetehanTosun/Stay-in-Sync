package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiEndpointQueryParamMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpoindQueryParamValueDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamDTO;
import de.unistuttgart.stayinsync.core.configuration.service.ApiEndpointQueryParamValueService;
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
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.net.URI;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/request-configuration/")
public class ApiEndpointQueryParamValueResource {

    @Inject
    ApiEndpointQueryParamValueService apiEndpointQueryParamValueService;

    @Inject
    ApiEndpointQueryParamMapper fullUpdateMapper;

    @Path("/{requestConfigId}/query-param-value")
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
                    name = "query-param-value",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = ApiEndpoindQueryParamValueDTO.class),
                            examples = @ExampleObject(name = "valid_endpoint_param_value", value = Examples.VALID_ENDPOINT_PARAM_VALUE_POST)
                    )
            )
            @PathParam("requestConfigId") Long requestConfigId,
            @Valid @NotNull ApiEndpoindQueryParamValueDTO apiEndpoindQueryParamValueDTO,
            @Context UriInfo uriInfo) {

        var persistedEndpointQueryParam = this.apiEndpointQueryParamValueService.persistConfiguration(apiEndpoindQueryParamValueDTO, requestConfigId);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedEndpointQueryParam.id));
        Log.debugf("New query-param-value created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }


    @DELETE
    @Operation(summary = "Deletes an exiting query-param")
    @APIResponse(
            responseCode = "204",
            description = "Delete a query-param"
    )
    @Path("/query-param-value/{id}")
    public void deleteEndpointQueryParam(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.apiEndpointQueryParamValueService.deleteQueryParamValue(id);
        Log.debugf("query-param-value with id %d deleted ", id);
    }

    @PUT
    @Path("/query-param-value/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates an exiting query-param-value by replacing it with the passed-in query-param-value")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the query-param-value"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid query-param-value passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No query-param-value found"
    )
    public Response fullyUpdateEndpointQueryParamValue(@Parameter(name = "id", required = true)
                                                       @RequestBody(
                                                               name = "query-param",
                                                               required = true,
                                                               content = @Content(
                                                                       mediaType = APPLICATION_JSON,
                                                                       schema = @Schema(implementation = ApiEndpointQueryParamDTO.class),
                                                                       examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_SYNCJOB)
                                                               )
                                                       )
                                                       @PathParam("id") Long id, @Valid @NotNull ApiEndpoindQueryParamValueDTO queryParamValue) {
        if (id != queryParamValue.id()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Id missmatch", "Make sure that the request body entity id matches the request parameter");
        }

        return this.apiEndpointQueryParamValueService.replaceConfiguration(queryParamValue)
                .map(updatedEndpointQueryParam -> {
                    Log.debugf("query-param-value replaced with new values %s", updatedEndpointQueryParam);
                    return Response.noContent().build();
                })
                .orElseGet(() -> {
                    Log.debugf("No query-param-value found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }
}
