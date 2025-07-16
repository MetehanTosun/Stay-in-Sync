package de.unistuttgart.stayinsync.core.configuration.rest;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.GraphStorageService;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphPersistenceResponseDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/transformation-rule")
@Produces(APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Transformation Rule", description = "Endpoints for managing logic graph transformation rules")
public class TransformationRuleResource {

    @Inject
    GraphStorageService graphService;

    @Inject
    ObjectMapper jsonObjectMapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a new Transformation Rule (Graph)")
    public Response createTransformationRule(GraphDTO dto, @Context UriInfo uriInfo) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BadRequestException("Graph name must not be empty.");
        }

        GraphStorageService.PersistenceResult result = graphService.persistGraph(dto);

        GraphPersistenceResponseDTO responseDto = new GraphPersistenceResponseDTO();

        dto.setStatus(result.entity().status);
        responseDto.setGraph(dto);
        responseDto.setErrors(result.validationErrors());

        var location = uriInfo.getAbsolutePathBuilder().path(Long.toString(result.entity().id)).build();

        return Response.created(location).entity(responseDto).build();
    }

    @GET
    @Operation(summary = "Returns all Transformation Rules")
    public List<GraphDTO> getAllTransformationRules() {

        return List.of();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a Transformation Rule for a given identifier")
    public GraphDTO getTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        LogicGraphEntity logicGraphEntity = graphService.findGraphById(id).orElseThrow(() -> new NotFoundException("TransformationRule with id " + id + " not found."));

        try {
            return jsonObjectMapper.readValue(logicGraphEntity.graphDefinitionJson, GraphDTO.class);
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates an existing Transformation Rule")
    public Response updateTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id, GraphDTO dto) {

        GraphStorageService.PersistenceResult result = graphService.updateGraph(id, dto);

        GraphPersistenceResponseDTO responseDto = new GraphPersistenceResponseDTO();

        dto.setStatus(result.entity().status);
        responseDto.setGraph(dto);
        responseDto.setErrors(result.validationErrors());

        Log.debugf("TransformationRule with id %d was updated.", id);

        return Response.ok(responseDto).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes an existing Transformation Rule")
    public Response deleteTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        boolean deleted = graphService.deleteGraphById(id);
        if (deleted) {
            Log.debugf("TransformationRule with id %d deleted.", id);
            return Response.noContent().build();
        } else {
            throw new NotFoundException("TransformationRule with id " + id + " not found for deletion.");
        }
    }
}
