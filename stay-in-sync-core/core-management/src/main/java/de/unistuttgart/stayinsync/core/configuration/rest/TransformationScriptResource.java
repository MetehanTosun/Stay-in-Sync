package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationScriptMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationScriptService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/transformation/{transformationId}/script")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Tag(name = "Transformation Script Configuration", description = "Endpoints for managing a transformation's script")
public class TransformationScriptResource {

    @Inject
    TransformationScriptService service;

    @Inject
    TransformationScriptMapper mapper;

    @GET
    @Operation(summary = "Gets the script for a specific transformation")
    public Response getScriptForTransformation(
            @Parameter(name = "transformationId", required = true) @PathParam("transformationId") Long transformationId
    ){
        return service.findByTransformationId(transformationId)
                .map(script -> Response.ok(mapper.mapToDTO(script)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Operation(summary = "Saves (creates or updates) the script for a specific transformation")
    public Response saveScriptForTransformation(
            @Parameter(name = "transformationId", required = true) @PathParam("transformationId") Long transformationId,
            TransformationScriptDTO dto) {

        var savedScript = service.saveOrUpdateForTransformation(transformationId, dto);
        return Response.ok(mapper.mapToDTO(savedScript)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes an existing transformation script")
    public Response deleteTransformationScript(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
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
