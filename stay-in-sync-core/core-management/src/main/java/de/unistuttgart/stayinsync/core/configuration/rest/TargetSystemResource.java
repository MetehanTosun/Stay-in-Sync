package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.TargetSystemMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TargetSystemService;
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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST resource for managing Target Systems within the configuration service.
 * Provides CRUD operations for creating, retrieving, updating, and deleting Target Systems.
 * Integrates with the TargetSystemService for persistence and the TargetSystemMapper for DTO conversion.
 */
@Path("/api/config/target-systems")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Tag(name = "TargetSystem Configuration", description = "Endpoints for managing TargetSystems")
public class TargetSystemResource {

    @Inject
    TargetSystemService service;

    @Inject
    TargetSystemMapper mapper;

    /**
     * Creates a new Target System and returns the created entity with its URI.
     *
     * @param dto DTO containing Target System configuration details.
     * @param uriInfo URI context used to build the location header for the created resource.
     * @return HTTP 201 response with the created Target System entity.
     */
    @POST
    @Operation(summary = "Creates a new TargetSystem")
    public Response createTargetSystem(TargetSystemDTO dto, @Context UriInfo uriInfo) {
        var created = service.createTargetSystem(dto);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(created.id()));
        Log.debugf("New TargetSystem created with URI %s", builder.build().toString());

        return Response.created(builder.build()).entity(created).build();
    }

    /**
     * Updates an existing Target System by its ID.
     * Validates that the ID in the path matches the ID in the provided DTO.
     *
     * @param id ID of the Target System to update.
     * @param dto Updated Target System DTO.
     * @return HTTP 200 response with the updated Target System entity.
     * @throws CoreManagementException if the path ID and DTO ID do not match.
     */
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

    /**
     * Retrieves a Target System by its ID.
     *
     * @param id ID of the Target System to retrieve.
     * @return HTTP 200 response with the Target System entity or HTTP 404 if not found.
     */
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

    /**
     * Retrieves all existing Target Systems.
     *
     * @return List of all Target System DTOs.
     */
    @GET
    @Operation(summary = "Returns all TargetSystems")
    public List<TargetSystemDTO> getAllTargetSystems() {
        var all = service.findAll();
        Log.debugf("Total number of TargetSystems: %d", all.size());
        return all;
    }

    /**
     * Deletes a Target System by its ID.
     * Logs success or warning depending on whether the deletion was successful.
     *
     * @param id ID of the Target System to delete.
     * @return HTTP 204 response if deleted, or throws HTTP 404 exception if not found.
     */
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
