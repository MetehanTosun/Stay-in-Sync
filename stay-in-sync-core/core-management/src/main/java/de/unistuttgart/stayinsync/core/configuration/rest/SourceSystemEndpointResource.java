package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemEndpointDTO;
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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("api/config/source-system/")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SourceSystemEndpointResource {

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    SourceSystemEndpointFullUpdateMapper fullUpdateMapper;

    @POST
    @Path("{sourceSystemId}/endpoint")
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
                            schema = @Schema(type = SchemaType.ARRAY, implementation = CreateSourceSystemEndpointDTO.class),
                            examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_ENDPOINT_CREATE)
                    )
            )
            @PathParam("sourceSystemId") Long sourceSystemId,
            @Valid @NotNull List<CreateSourceSystemEndpointDTO> sourceSystemEndpointDTO,
            @Context UriInfo uriInfo) {
        var persistedSourceSystemEndpoints = this.sourceSystemEndpointService.persistSourceSystemEndpointList(sourceSystemEndpointDTO, sourceSystemId);
        Log.infof("New source-system-endpoints created for source system %d", sourceSystemId.toString());

        return Response.status(Response.Status.CREATED).entity(fullUpdateMapper.mapToDTOList(persistedSourceSystemEndpoints)).build();
    }

    @GET
    @Path("{sourceSystemId}/endpoint")
    @Operation(summary = "Returns all the source-system-endpoints for a specified system from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all source-system-endpoints for specified system",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = SourceSystemEndpointDTO.class, type = SchemaType.ARRAY)
            )
    )
    public List<SourceSystemEndpointDTO> getAllSourceSystemEndpoints(@Parameter(name = "source system", description = "The id of the associated source system") @PathParam("sourceSystemId") Long sourceSystemid) {
        var sourceSystemEndpoints = this.sourceSystemEndpointService.findAllEndpointsWithSourceSystemIdLike(sourceSystemid);

        Log.infof("Total number of source-system-endpoints: %d", sourceSystemEndpoints.size());

        return fullUpdateMapper.mapToDTOList(sourceSystemEndpoints);
    }


    @GET
    @Path("/endpoint/{id}")
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
                    Log.infof("Found source-system-endpoint: %s", sourceSystemEndpoint);
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
    @Path("/endpoint/{id}")
    public void deleteSourceSystemEndpoint(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.sourceSystemEndpointService.deleteSourceSystemEndpointById(id);
        Log.infof("source-system-endpoint with id %d deleted ", id);
    }

    @PUT
    @Path("/endpoint/{id}")
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
                                                                    examples = @ExampleObject(name = "valid endpoint", value = Examples.VALID_SOURCE_SYSTEM_ENDPOINT_POST)
                                                            )
                                                    )
                                                    @PathParam("id") Long id, @Valid @NotNull CreateSourceSystemEndpointDTO sourceSystemEndpointDTO) {

        return this.sourceSystemEndpointService.replaceSourceSystemEndpoint(fullUpdateMapper.mapToEntity(sourceSystemEndpointDTO))
                .map(updatedSourceSystemEndpoint -> {
                    Log.infof("source-system-endpoint replaced with new values %s", updatedSourceSystemEndpoint);
                    return Response.noContent().build();
                })
                .orElseGet(() -> {
                    Log.infof("No source-system-endpoint found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }

}
