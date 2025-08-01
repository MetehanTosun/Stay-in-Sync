package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.RequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.CreateArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.GetRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TargetSystemApiRequestConfigurationService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/target-arc")
@Produces(APPLICATION_JSON)
@Tag(name = "Target ARC Configuration", description = "Endpoints for managing Target System API Request Configurations (ARCs)")
public class TargetSystemApiRequestConfigurationResource {

    @Inject
    TargetSystemApiRequestConfigurationService service;

    @Inject
    RequestConfigurationMapper mapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a new Target ARC")
    public Response createTargetArc(@Valid CreateArcDTO dto, @Context UriInfo uriInfo) {
        var persistedArc = service.create(dto);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedArc.id));
        Log.debugf("New Target ARC created with URI %s", builder.build().toString());

        return Response.created(builder.build()).entity(mapper.mapToGetDTO(persistedArc)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a Target ARC by its ID")
    public Response getTargetArc(@PathParam("id") Long id) {
        return service.findById(id)
                .map(arc -> Response.ok(mapper.mapToGetDTO(arc)).build())
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND,
                        "Target ARC not found", "No Target ARC found with id %d", id));
    }

    @GET
    @Path("/by-system/{targetSystemId}")
    @Operation(summary = "Returns all Target ARCs for a given Target System")
    public List<GetRequestConfigurationDTO> getArcsByTargetSystem(@PathParam("targetSystemId") Long targetSystemId) {
        var arcs = service.findAllByTargetSystemId(targetSystemId);
        return mapper.mapToGetDTOList(arcs);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes a Target ARC by its ID")
    public Response deleteTargetArc(@PathParam("id") Long id) {
        if (service.deleteById(id)) {
            Log.debugf("Target ARC with id %d deleted.", id);
            return Response.noContent().build();
        } else {
            throw new CoreManagementException(Response.Status.NOT_FOUND,
                    "Target ARC not found", "Target ARC with id %d not found for deletion.", id);
        }
    }
}
