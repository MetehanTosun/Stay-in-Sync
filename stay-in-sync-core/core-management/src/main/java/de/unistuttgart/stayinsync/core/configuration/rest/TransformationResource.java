package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationAssemblyDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationDetailsDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/transformation")
@Produces(APPLICATION_JSON)
@Tag(name = "Transformation Configuration", description = "Endpoints for managing transformations")
public class TransformationResource {

    @Inject
    TransformationService service;

    @Inject
    TransformationMapper mapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a new transformation shell",
            description = "Creates the initial transformation object with basic info like name and description. Returns the created object with its new ID.")
    public Response createTransformationShell(TransformationShellDTO dto, @Context UriInfo uriInfo) {
        var persisted = service.createShell(dto);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persisted.id));
        Log.debugf("New transformation shell created with URI %s", builder.build().toString());

        // Return the full details DTO so the frontend has the initial state
        return Response.created(builder.build()).entity(mapper.mapToDetailsDTO(persisted)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Assembles a transformation with its related components",
            description = "Updates an existing transformation shell by linking it to a script, rule, and endpoints using their IDs.")
    public Response assembleTransformation(@Parameter(name = "id", required = true) @PathParam("id") Long id, TransformationAssemblyDTO dto) {
        if (!id.equals(dto.id())) {
            throw new CoreManagementWebException(Response.Status.BAD_REQUEST, "ID Mismatch",
                    "The ID in the path (%d) does not match the ID in the request body (%d).", id, dto.id());
        }

        var updated = service.assemble(id, dto);
        Log.debugf("Transformation with id %d was assembled.", id);
        return Response.ok(mapper.mapToDetailsDTO(updated)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a transformation for a given identifier")
    public Response getTransformation(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return service.findById(id)
                .map(transformation -> {
                    Log.debugf("Found transformation: %s", transformation);
                    return Response.ok(mapper.mapToDetailsDTO(transformation)).build();
                })
                .orElseThrow(() -> new CoreManagementWebException(Response.Status.NOT_FOUND, "Unable to find transformation", "No transformation found using id %d", id));
    }

    @GET
    @Operation(summary = "Returns all transformations")
    public List<TransformationDetailsDTO> getAllTransformations() {
        var transformations = service.findAll();
        Log.debugf("Total number of transformations: %d", transformations.size());
        return transformations.stream()
                .map(mapper::mapToDetailsDTO)
                .collect(Collectors.toList());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes an existing transformation")
    public Response deleteTransformation(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        service.delete(id);
        Log.debugf("Transformation with id %d deleted", id);
        return Response.noContent().build();
    }
}
