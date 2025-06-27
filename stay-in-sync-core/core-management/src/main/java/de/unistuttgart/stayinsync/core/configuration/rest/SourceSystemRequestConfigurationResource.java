package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
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
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("api/source-system/{sourceSystemId}/request-configuration")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SourceSystemRequestConfigurationResource {
    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    SourceSystemEndpointFullUpdateMapper fullUpdateMapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a valid source-system-endpoint")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created source-system-endpoint",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid source-system-endpoint passed in (or no request body found)"
    )
    public Response createSourceSystemEndpoint(
            @RequestBody(
                    name = "source-system-endpoint",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = SourceSystemEndpointDTO.class),
                            examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_SYNCJOB_TO_CREATE)
                    )
            )
            @PathParam("sourceSystemId") Long sourceSystemId,
            @Valid @NotNull SourceSystemEndpointDTO SourceSystemEndpointDTO,
            @Context UriInfo uriInfo) {
//        SourceSystem sourceSystem = this.sourceSystemService.findSourceSystemById(sourceSystemId).orElseThrow(() ->
//        {
//            return new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find source-system", "Could not find source-system for id %s", sourceSystemId);
//        });
        var persistedSourceSystemEndpoint = this.sourceSystemEndpointService.persistSourceSystemEndpoint(fullUpdateMapper.mapToEntity(SourceSystemEndpointDTO), sourceSystemId);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedSourceSystemEndpoint.id));
        Log.debugf("New source-system-endpoint created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }

    @GET
    @Operation(summary = "Returns all the source-system-endpoints from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all source-system-endpoints",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = SourceSystemEndpointDTO.class, type = SchemaType.ARRAY)
            )
    )
    public List<SourceSystemEndpointDTO> getAllSourceSystemEndpoints(@Parameter(name = "source_system_filter", description = "An optional filter parameter to filter results by source system id") @QueryParam("source_system_id") Optional<Long> sourceSystemFilter) {
        var sourceSystemEndpoints = sourceSystemFilter
                .map(this.sourceSystemEndpointService::findAllEndpointsWithSourceSystemIdLike)
                .orElseGet(this.sourceSystemEndpointService::findAllSourceSystemEndpoints);

        Log.debugf("Total number of source-system-endpoints: %d", sourceSystemEndpoints.size());

        return fullUpdateMapper.mapToDTOList(sourceSystemEndpoints);
    }


    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a source-system-endpoint for a given identifier")
    @APIResponse(
            responseCode = "200",
            description = "Gets a source-system-endpoint for a given id",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = SourceSystemEndpointDTO.class),
                    examples = @ExampleObject(name = "source-system-endpoint", value = Examples.VALID_EXAMPLE_SYNCJOB)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The source-system-endpoint is not found for a given identifier"
    )
    public Response getSourceSystemEndpoint(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.sourceSystemEndpointService.findSourceSystemEndpointById(id)
                .map(sourceSystemEndpoint -> {
                    Log.debugf("Found source-system-endpoint: %s", sourceSystemEndpoint);
                    return Response.ok(fullUpdateMapper.mapToDTO(sourceSystemEndpoint)).build();
                })
                .orElseThrow(() -> {
                    Log.warnf("No source-system-endpoint found using id %d", id);
                    return new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find source-system-endpoint", "No source-system-endpoint found using id %d", id);
                });
    }

    @DELETE
    @Operation(summary = "Deletes an exiting source-system-endpoint")
    @APIResponse(
            responseCode = "204",
            description = "Delete a source-system-endpoint"
    )
    @Path("/{id}")
    public void deleteSourceSystemEndpoint(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.sourceSystemEndpointService.deleteSourceSystemEndpointById(id);
        Log.debugf("source-system-endpoint with id %d deleted ", id);
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates an exiting source-system-endpoint by replacing it with the passed-in source-system-endpoint")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the source-system-endpoint"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid source-system-endpoint passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No source-system-endpoint found"
    )
    public Response fullyUpdateSourceSystemEndpoint(@Parameter(name = "id", required = true)
                                                    @RequestBody(
                                                            name = "source-system-endpoint",
                                                            required = true,
                                                            content = @Content(
                                                                    mediaType = APPLICATION_JSON,
                                                                    schema = @Schema(implementation = SourceSystemEndpointDTO.class),
                                                                    examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_SYNCJOB)
                                                            )
                                                    )
                                                    @PathParam("id") Long id, @Valid @NotNull SourceSystemEndpointDTO SourceSystemEndpointDTO) {
        if (id != SourceSystemEndpointDTO.id()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Id missmatch", "Make sure that the request body entity id matches the request parameter");
        }

        return this.sourceSystemEndpointService.replaceSourceSystemEndpoint(fullUpdateMapper.mapToEntity(SourceSystemEndpointDTO))
                .map(updatedSourceSystemEndpoint -> {
                    Log.debugf("source-system-endpoint replaced with new values %s", updatedSourceSystemEndpoint);
                    return Response.noContent().build();
                })
                .orElseGet(() -> {
                    Log.debugf("No source-system-endpoint found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }
}
