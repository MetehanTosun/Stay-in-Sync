package de.unistuttgart.stayinsync.core.configuration.rest;


import de.unistuttgart.stayinsync.syncnode.logik_engine.database.DTOs.GraphDTO;
import de.unistuttgart.stayinsync.syncnode.logik_engine.database.GraphMapperService;
import de.unistuttgart.stayinsync.syncnode.logik_engine.database.GraphStorageService;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
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

import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/transformation-rule")
@Produces(APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Transformation Rule", description = "Endpoints for managing logic graph transformation rules")
public class TransformationRuleResource {

    @Inject
    GraphStorageService graphService;

    @Inject
    GraphMapperService graphMapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a new Transformation Rule (Graph)")
    public Response createTransformationRule(GraphDTO dto, @Context UriInfo uriInfo) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BadRequestException("Graph name must not be empty.");
        }

        String graphName = dto.getName();

        List<Node> nodeGraph = graphMapper.toNodeGraph(dto);
        var persisted = graphService.persistGraph(graphName, nodeGraph);

        var location = uriInfo.getAbsolutePathBuilder().path(Long.toString(persisted.id)).build();
        Log.debugf("New TransformationRule created with URI %s", location);

        return Response.created(location).entity(dto).build();
    }

    @GET
    @Operation(summary = "Returns all Transformation Rules")
    public List<GraphDTO> getAllTransformationRules() {
        List<List<Node>> allGraphs = graphService.findAllGraphs();
        Log.debugf("Found %d total transformation rules.", allGraphs.size());

        List<GraphDTO> allGraphDtos = new ArrayList<>();
        for (List<Node> graph : allGraphs) {
            allGraphDtos.add(graphMapper.graphToDto(graph));
        }

        return allGraphDtos;
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a Transformation Rule for a given identifier")
    public GraphDTO getTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        List<Node> graph = graphService.findGraphById(id)
                .orElseThrow(() -> new NotFoundException("TransformationRule with id " + id + " not found."));

        Log.debugf("Found TransformationRule with id: %d", id);
        return graphMapper.graphToDto(graph);
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates an existing Transformation Rule")
    public Response updateTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id, GraphDTO dto) {
        List<Node> nodeGraph = graphMapper.toNodeGraph(dto);

        graphService.updateGraph(id, nodeGraph)
                .orElseThrow(() -> new NotFoundException("TransformationRule with id " + id + " not found."));

        Log.debugf("TransformationRule with id %d was updated.", id);
        return Response.ok(dto).build();
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
