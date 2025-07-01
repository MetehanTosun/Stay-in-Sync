package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationScriptMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationScriptService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/transformation-script")
@Produces(APPLICATION_JSON)
public class TransformationScriptResource {

    @Inject
    TransformationScriptService service;

    @Inject
    TransformationScriptMapper mapper;

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a transformation script for a given identifier")
    public Response getScript(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return service.findById(id)
                .map(script -> {
                    Log.debugf("Found transformation script: %s", script);
                    return Response.ok(mapper.mapToDTO(script)).build();
                })
                .orElseThrow(() -> new CoreManagementWebException(Response.Status.NOT_FOUND, "Unable to find script", "No transformation script found using id %d", id));
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates an existing transformation script")
    public Response updateScript(@Parameter(name = "id", required = true) @PathParam("id") Long id,
                                 TransformationScriptDTO dto) {
        if (!id.equals(dto.id())) {
            throw new CoreManagementWebException(Response.Status.BAD_REQUEST, "ID mismatch", "The ID in the path does not match the ID in the request body.");
        }

        return service.update(id, mapper.mapToEntity(dto))
                .map(updatedScript -> {
                    Log.debugf("Script with id %d was updated.", id);
                    return Response.ok(mapper.mapToDTO(updatedScript)).build();
                })
                .orElseThrow(() -> new CoreManagementWebException(Response.Status.NOT_FOUND, "Unable to find script", "No transformation script found for update with id %d", id));
    }
}
