package de.unistuttgart.stayinsync.core.configuration.rest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.MultipartForm;

import jakarta.ws.rs.core.MediaType;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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

@Path("api/config/source-system")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
/**
 * REST resource for managing Source Systems and their endpoints.
 * Provides CRUD operations and OpenAPI-based endpoint discovery.
 */
public class SourceSystemResource {

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    SourceSystemFullUpdateMapper sourceSystemFullUpdateMapper;

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @Inject
    SourceSystemEndpointFullUpdateMapper endpointMapper;

    @GET
    @Operation(summary = "Returns all source systems")
    @APIResponse(responseCode = "200", description = "List of all source systems", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = SourceSystem.class)))
    public List<SourceSystemDTO> getAllSs() {
        return sourceSystemFullUpdateMapper.mapToDTOList(sourceSystemService.findAllSourceSystems());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a source system by its ID")
    @APIResponse(responseCode = "200", description = "The found source system", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystemDTO.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response getSsById(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        SourceSystem found = sourceSystemService.findSourceSystemById(id)
                .orElseThrow(
                        () -> new CoreManagementException(
                                Response.Status.NOT_FOUND,
                                "Source system not found",
                                "No source system found with id %d", id));
        Log.debugf("Found source system: %s", found);
        return Response.ok(sourceSystemFullUpdateMapper.mapToDTO(found)).build();
    }
    // Multipart create
    @POST
    @Operation(summary = "Creates a new source system")
    @APIResponse(responseCode = "201", description = "The URI of the created source system", headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class)))
    @APIResponse(responseCode = "400", description = "Invalid source system passed in")
    public Response createSs(
            @RequestBody(name = "source-system", required = true, content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = CreateSourceSystemDTO.class), examples = @ExampleObject(name = "valid_source_system_create", value = Examples.VALID_SOURCE_SYSTEM_CREATE))) @Valid @NotNull CreateSourceSystemDTO sourceSystemDTO,
            @Context UriInfo uriInfo) {
        SourceSystem sourceSystem = sourceSystemService.createSourceSystem(sourceSystemDTO);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(sourceSystem.id));
        Log.debugf("New source system created with URI %s", builder.build().toString());
        return Response.created(builder.build()).build();
        // TODO: Throw Exception in case of invalid source system: we need to know how
        // the source system looks like first(final model)
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Fully updates an existing source system")
    @APIResponse(responseCode = "200", description = "The updated source system", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = CreateSourceSystemDTO.class), examples = @ExampleObject(name = "valid_source_system_create", value = Examples.VALID_SOURCE_SYSTEM_CREATE)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response updateSs(@Parameter(name = "id", required = true) @PathParam("id") Long id,
                             @Valid @NotNull CreateSourceSystemDTO sourceSystemDTO) {
//        sourceSystemDTO.id() = id;
        return sourceSystemService.updateSourceSystem(sourceSystemDTO)
                .map(updated -> Response.ok(sourceSystemFullUpdateMapper.mapToDTO(updated)).build())
                .orElseThrow(() -> new CoreManagementException(
                        Response.Status.NOT_FOUND,
                        "Source system not found",
                        "No source system found with id %d", id));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes a source system by its ID")
    @APIResponse(responseCode = "204", description = "Source system deleted")
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response deleteSs(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        if (!sourceSystemService.deleteSourceSystemById(id)) {
            throw new CoreManagementException(
                    Response.Status.NOT_FOUND,
                    "Source system not found",
                    "No source system found with id %d", id);
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/endpoints")
    @Operation(summary = "List all endpoints for a source system")
    @APIResponse(responseCode = "200", description = "List of endpoints",
      content = @Content(mediaType = APPLICATION_JSON,
                         schema = @Schema(type = SchemaType.ARRAY, implementation = SourceSystemEndpointDto.class)))
    /**
     * Lists all endpoints configured for a given source system.
     *
     * @param sourceId the ID of the source system
     * @return list of endpoint DTOs for the system
     */
    public List<SourceSystemEndpointDto> listEndpoints(@PathParam("id") Long sourceId) {
        return endpointService.listBySourceId(sourceId)
                              .stream()
                              .map(endpointMapper::toDto)
                              .collect(Collectors.toList());
    }

    @POST
    @Path("/{id}/endpoints")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Create a new endpoint for a source system")
    @APIResponse(responseCode = "201", description = "Endpoint created",
      headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class)))
    /**
     * Creates a new endpoint for a given source system.
     *
     * @param sourceId the ID of the system
     * @param input the DTO with endpoint path and HTTP method
     * @param uriInfo context for generating Location header
     * @return HTTP 201 with created endpoint DTO and Location header
     */
    public Response createEndpoint(
        @PathParam("id") Long sourceId,
        @Valid @NotNull SourceSystemEndpointDto input,
        @Context UriInfo uriInfo
    ) {
        var entity = endpointService.createEndpoint(sourceId, input.endpointPath(), input.httpRequestType());
        var dto = endpointMapper.toDto(entity);
        URI location = uriInfo.getAbsolutePathBuilder()
                              .path(dto.id().toString())
                              .build();
        return Response.created(location).entity(dto).build();
    }

    @GET
    @Path("/{id}/discover")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Discover endpoints from OpenAPI spec")
    @APIResponse(responseCode = "200", description = "List of discovered endpoints",
      content = @Content(mediaType = APPLICATION_JSON,
                         schema = @Schema(type = SchemaType.ARRAY, implementation = DiscoveredEndpoint.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    /**
     * Discovers endpoints from the stored OpenAPI specification for a source system.
     *
     * @param sourceId the ID of the system
     * @return HTTP 200 with list of discovered endpoints, or 404 if system not found
     */
    public Response discoverEndpoints(@PathParam("id") Long sourceId) {
        var list = endpointService.discoverAllEndpoints(sourceId);
        return Response.ok(list).build();
    }
}