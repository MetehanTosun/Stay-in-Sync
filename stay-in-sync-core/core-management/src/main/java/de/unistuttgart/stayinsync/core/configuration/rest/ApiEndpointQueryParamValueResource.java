package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.ApiEndpointQueryParamValueMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamValueDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamDTO;
import de.unistuttgart.stayinsync.core.configuration.service.ApiEndpointQueryParamValueService;
import de.unistuttgart.stayinsync.core.configuration.rest.Examples;
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
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ApiEndpointQueryParamValueResource {

    @Inject
    ApiEndpointQueryParamValueService apiEndpointQueryParamValueService;

    @Inject
    ApiEndpointQueryParamValueMapper fullUpdateMapper;

    @Path("/{requestConfigId}/query-param-value")
    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a valid query-param-value for the specified endpoint")
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
                            schema = @Schema(implementation = ApiEndpointQueryParamValueDTO.class),
                            examples = @ExampleObject(name = "valid_endpoint_param_value", value = Examples.VALID_ENDPOINT_PARAM_VALUE_POST)
                    )
            )
            @PathParam("requestConfigId") Long requestConfigId,
            @Valid @NotNull ApiEndpointQueryParamValueDTO paramValueDTO,
            @Context UriInfo uriInfo) {

        var persistedEndpointQueryParam = this.apiEndpointQueryParamValueService.persistValue(paramValueDTO, requestConfigId);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedEndpointQueryParam.id));
        Log.debugf("New query-param-value created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }

    @GET
    @Path("/{requestConfigId}/query-param-value")
    @Operation(summary = "Returns all the query-parameter-values for the specified source-system from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all query parameter values",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = ApiEndpointQueryParamDTO.class, type = SchemaType.ARRAY)
            )
    )
    public List<ApiEndpointQueryParamValueDTO> getAllQueryParams(@Parameter(name = "request config", description = "Associated request config") @PathParam("requestConfigId") Long requestConfigId) {
        var apiRequestHeaders = this.apiEndpointQueryParamValueService.findQueryParamValueByRequestConfig(requestConfigId);

        Log.debugf("Total number of query-parameter-values: %d", apiRequestHeaders.size());

        return fullUpdateMapper.mapToDTOList(apiRequestHeaders);
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
                                                       @PathParam("id") Long id, @Valid @NotNull ApiEndpointQueryParamValueDTO queryParamValue) {
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
