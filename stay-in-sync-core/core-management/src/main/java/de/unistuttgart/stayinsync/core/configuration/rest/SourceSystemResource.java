package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
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

@Path("api/config/source-system")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
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

    @GET
    @Path("/systemNames")
    public Response getAllSourceSystemNames() {
        List<String> names = SourceSystem.<SourceSystem>streamAll()
                .map(system -> system.name)
                .toList();

        return Response.ok(names).build();
    }

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
        return Response.created(builder.build()).entity(sourceSystemFullUpdateMapper.mapToDTO(sourceSystem)).build();
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
        // Create a new DTO with the correct ID from the path parameter
        CreateSourceSystemDTO updatedDTO = new CreateSourceSystemDTO(
            id,
            sourceSystemDTO.name(),
            sourceSystemDTO.apiUrl(),
            sourceSystemDTO.description(),
            sourceSystemDTO.apiType(),
            sourceSystemDTO.aasId(),
            sourceSystemDTO.apiAuthType(),
            sourceSystemDTO.authConfig(),
            sourceSystemDTO.openApiSpec()
        );
        
        return sourceSystemService.updateSourceSystem(updatedDTO)
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
}