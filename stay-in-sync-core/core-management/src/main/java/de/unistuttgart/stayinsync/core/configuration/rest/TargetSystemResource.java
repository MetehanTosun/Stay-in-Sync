package de.unistuttgart.stayinsync.core.configuration.rest;

import static jakarta.ws.rs.core.MediaType.*;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.TargetSystemMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TargetSystemService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/target-systems")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Tag(name = "TargetSystem Configuration", description = "Endpoints for managing TargetSystems")
public class TargetSystemResource {

    @Inject
    TargetSystemService service;

    @Inject
    TargetSystemMapper mapper;

    @POST
    @Operation(summary = "Creates a new TargetSystem")
    public Response createTargetSystem(TargetSystemDTO dto, @Context UriInfo uriInfo) {
        var created = service.createTargetSystem(dto);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(created.id()));
        Log.debugf("New TargetSystem created with URI %s", builder.build().toString());

        return Response.created(builder.build()).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Updates an existing TargetSystem")
    public Response updateTargetSystem(@Parameter(name = "id", required = true) @PathParam("id") Long id,
            TargetSystemDTO dto) {
        if (!id.equals(dto.id())) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "ID Mismatch",
                    "The ID in the path (%d) does not match the ID in the request body (%d).", id, dto.id());
        }

        var updated = service.updateTargetSystem(id, dto);
        Log.debugf("TargetSystem with id %d updated", id);
        return Response.ok(updated).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a TargetSystem by its ID")
    public Response getTargetSystem(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return service.findById(id)
                .map(entity -> {
                    Log.debugf("Found TargetSystem: %s", entity);
                    return Response.ok(mapper.toDto(entity)).build();
                })
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "TargetSystem not found",
                        "No TargetSystem found using id %d", id));
    }

    @GET
    @Operation(summary = "Returns all TargetSystems")
    public List<TargetSystemDTO> getAllTargetSystems() {
        var all = service.findAll();
        Log.debugf("Total number of TargetSystems: %d", all.size());
        return all;
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes a TargetSystem")
    public Response deleteTargetSystem(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        boolean deleted = service.delete(id);

        if (deleted) {
            Log.debugf("TargetSystem with id %d deleted", id);
            return Response.noContent().build();
        } else {
            Log.warnf("Attempted to delete non-existent TargetSystem with id %d", id);
            throw new CoreManagementException(Response.Status.NOT_FOUND,
                    "TargetSystem not found",
                    "TargetSystem with id %d could not be found for deletion.", id);
        }
    }
}
