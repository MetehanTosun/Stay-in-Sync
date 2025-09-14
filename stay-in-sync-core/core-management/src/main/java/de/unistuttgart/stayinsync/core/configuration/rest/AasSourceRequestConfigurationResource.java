package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSourceApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.mapping.AasApiRequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.CreateAasArcDTO;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasApiRequestConfigurationService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

@Path("/api/config/aas-request-configuration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AAS Request Configuration", description = "Endpoints for managing AAS-based Source ARCs")
public class AasSourceRequestConfigurationResource {

    @Inject
    AasApiRequestConfigurationService aasArcService;

    @Inject
    AasApiRequestConfigurationMapper aasArcMapper;

    @POST
    @Operation(summary = "Creates a new AAS Source ARC", description = "Links a Submodel from an AAS Source System to a script alias.")
    @APIResponse(responseCode = "201", description = "AAS ARC created successfully. Returns the created ARC.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AasArcDTO.class)))
    @APIResponse(responseCode = "400", description = "Invalid data provided in the request body.")
    @APIResponse(responseCode = "404", description = "The specified Source System or Submodel was not found.")
    public Response createAasSourceArc(
            @RequestBody(description = "The AAS ARC to create.", required = true,
                    content = @Content(schema = @Schema(implementation = CreateAasArcDTO.class)))
            @Valid CreateAasArcDTO arcDto,
            @Context UriInfo uriInfo) {

        AasSourceApiRequestConfiguration persistedArc = aasArcService.create(arcDto);
        AasArcDTO responseDto = aasArcMapper.mapToDto(persistedArc);

        URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedArc.id)).build();
        Log.debugf("New AAS ARC created with URI %s", location);

        return Response.created(location).entity(responseDto).build();
    }

    @GET
    @Path("/by-source-system/{id}")
    @Operation(summary = "Get all AAS ARCs for a Source System", description = "Retrieves a list of all AAS-based Source ARCs configured for a given Source System ID.")
    @APIResponse(responseCode = "200", description = "A list of AAS ARCs.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AasArcDTO.class)))
    public List<AasArcDTO> getBySourceSystem(
            @Parameter(description = "ID of the Source System", required = true)
            @PathParam("id") Long sourceSystemId) {

        return aasArcService.findBySourceSystemId(sourceSystemId);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an AAS ARC by its ID")
    @APIResponse(responseCode = "200", description = "The requested AAS ARC.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AasArcDTO.class)))
    @APIResponse(responseCode = "404", description = "No AAS ARC found with the given ID.")
    public AasArcDTO getById(
            @Parameter(description = "ID of the AAS ARC", required = true)
            @PathParam("id") Long id) {

        return aasArcService.findById(id)
                .map(aasArcMapper::mapToDto)
                .orElseThrow(() -> new NotFoundException("AAS ARC with id " + id + " not found."));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update an existing AAS ARC")
    @APIResponse(responseCode = "200", description = "AAS ARC updated successfully.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AasArcDTO.class)))
    @APIResponse(responseCode = "400", description = "Invalid data provided.")
    @APIResponse(responseCode = "404", description = "No AAS ARC found with the given ID.")
    public AasArcDTO updateAasSourceArc(
            @Parameter(description = "ID of the AAS ARC to update", required = true)
            @PathParam("id") Long id,
            @RequestBody(description = "The updated AAS ARC data.", required = true,
                    content = @Content(schema = @Schema(implementation = CreateAasArcDTO.class)))
            @Valid CreateAasArcDTO arcDto) {

        AasSourceApiRequestConfiguration updatedArc = aasArcService.update(id, arcDto);
        return aasArcMapper.mapToDto(updatedArc);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an AAS ARC")
    @APIResponse(responseCode = "204", description = "AAS ARC deleted successfully.")
    @APIResponse(responseCode = "404", description = "No AAS ARC found with the given ID.")
    public Response deleteAasSourceArc(
            @Parameter(description = "ID of the AAS ARC to delete", required = true)
            @PathParam("id") Long id) {

        aasArcService.delete(id);
        return Response.noContent().build();
    }
}
