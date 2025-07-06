package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationScriptMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationDetailsDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationScriptService;
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

@Path("/api/config/transformation-script")
@Produces(APPLICATION_JSON)
@Tag(name = "Transformation Script Configuration", description = "Endpoints for managing transformation scripts")
public class TransformationScriptResource {

    @Inject
    TransformationScriptService service;

    @Inject
    TransformationScriptMapper mapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a new transformation script",
            description = "Creates the transformation script. Returns the created script with its new ID.")
    public Response createScript(TransformationScriptDTO dto, @Context UriInfo uriInfo) {
        var persisted = service.create(dto);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persisted.id));
        Log.debugf("New transformation script created with URI %s", builder.build().toString());

        return Response.created(builder.build()).entity(mapper.mapToDTO(persisted)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a transformation script for a given identifier")
    public Response getScript(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return service.findById(id)
                .map(script -> {
                    Log.debugf("Found transformation script: %s", script);
                    return Response.ok(mapper.mapToDTO(script)).build();
                })
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find script", "No transformation script found using id %d", id));
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates an existing transformation script")
    public Response updateScript(@Parameter(name = "id", required = true) @PathParam("id") Long id,
                                 TransformationScriptDTO dto) {
        if (!id.equals(dto.id())) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "ID mismatch", "The ID in the path does not match the ID in the request body.");
        }

        return service.update(id, mapper.mapToEntity(dto))
                .map(updatedScript -> {
                    Log.debugf("Script with id %d was updated.", id);
                    return Response.ok(mapper.mapToDTO(updatedScript)).build();
                })
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find script", "No transformation script found for update with id %d", id));
    }

    @GET
    @Operation(summary = "Returns all transformation scripts")
    public List<TransformationScriptDTO> getAllTransformations() {
        var transformations = service.findAll();
        Log.debugf("Total number of transformation scripts: %d", transformations.size());
        return transformations.stream()
                .map(mapper::mapToDTO)
                .collect(Collectors.toList());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes an existing transformation script")
    public Response deleteTransformation(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        boolean deleted = service.delete(id);

        if (deleted) {
            Log.debugf("Transformation script with id %d deleted", id);
            return Response.noContent().build();
        } else {
            Log.warnf("Attempted to delete non-existent transformation script with id %d", id);
            throw new CoreManagementException(Response.Status.NOT_FOUND,
                    "Transformation script not found",
                    "Transformation script with id %d could not be found for deletion.", id);
        }
    }
}
