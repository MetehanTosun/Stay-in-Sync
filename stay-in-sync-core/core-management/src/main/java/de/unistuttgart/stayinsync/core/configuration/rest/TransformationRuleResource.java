package de.unistuttgart.stayinsync.core.configuration.rest;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.OperatorMetadataDTO;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.GraphStorageService;
import de.unistuttgart.stayinsync.core.configuration.util.OperatorMetadata;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphPersistenceResponseDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VFlowGraphDTO;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphMapper;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.ValidationError;
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
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/transformation-rule")
@Produces(APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Transformation Rule", description = "Endpoints for managing logic graph transformation rules")
public class TransformationRuleResource {

    @Inject
    GraphStorageService graphService;

    @Inject
    GraphMapper graphMapper;

    @Inject
    ObjectMapper jsonObjectMapper;

    @Inject
    OperatorMetadata operatorMetadataService;


    @GET
    @Path("/operators")
    @Operation(summary = "Returns metadata for all available logic operators",
            description = "Provides a list of all operators with their signature (description, input/output types) for use in a UI graph editor.")
    public List<OperatorMetadataDTO> getAvailableOperators() {
        Log.debug("Fetching all available operator metadata for UI.");
        return operatorMetadataService.findAllOperatorMetadata();
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a new Transformation Rule (Graph) from VFlow data")
    public Response createTransformationRule(VFlowGraphDTO vflowDto, @Context UriInfo uriInfo) {
        if (vflowDto.getName() == null || vflowDto.getName().trim().isEmpty()) {
            throw new BadRequestException("Graph name must not be empty.");
        }

        GraphDTO graphForPersistence = graphMapper.vflowToGraphDto(vflowDto);

        GraphStorageService.PersistenceResult result = graphService.persistGraph(graphForPersistence);

        GraphPersistenceResponseDTO responseDto = new GraphPersistenceResponseDTO();
        graphForPersistence.setId(result.entity().id);
        graphForPersistence.setStatus(result.entity().status);
        responseDto.setGraph(graphForPersistence);
        responseDto.setErrors(result.validationErrors());

        var location = uriInfo.getAbsolutePathBuilder().path(Long.toString(result.entity().id)).build();
        Log.debugf("New TransformationRule created with URI %s", location);

        return Response.created(location).entity(responseDto).build();
    }

    @GET
    @Operation(summary = "Returns all Transformation Rules")
    public List<GraphPersistenceResponseDTO> getAllTransformationRules() {
        List<LogicGraphEntity> entities = graphService.getAllGraphs();
        List<GraphPersistenceResponseDTO> responseDtos = new ArrayList<>();

        for (LogicGraphEntity entity : entities) {
            try {
                GraphDTO graphDto = jsonObjectMapper.readValue(entity.graphDefinitionJson, GraphDTO.class);
                graphDto.setId(entity.id);
                graphDto.setName(entity.name);
                graphDto.setStatus(entity.status);

                List<ValidationError> errors;
                if (entity.status == GraphStatus.DRAFT && entity.validationErrorsJson != null) {
                    errors = jsonObjectMapper.readValue(entity.validationErrorsJson, new TypeReference<List<ValidationError>>() {});
                } else {
                    errors = new ArrayList<>();
                }

                GraphPersistenceResponseDTO responseDto = new GraphPersistenceResponseDTO(graphDto, errors);
                responseDtos.add(responseDto);

            } catch (JsonProcessingException e) {
                Log.errorf(e, "Failed to parse data for entity id %d", entity.id);
            }
        }

        return responseDtos;
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
    @Operation(summary = "Updates an existing Transformation Rule from VFlow data")
    public Response updateTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id, VFlowGraphDTO vflowDto) {
        GraphDTO graphForPersistence = graphMapper.vflowToGraphDto(vflowDto);

        GraphStorageService.PersistenceResult result = graphService.updateGraph(id, graphForPersistence);

        GraphPersistenceResponseDTO responseDto = new GraphPersistenceResponseDTO();
        graphForPersistence.setId(result.entity().id);
        graphForPersistence.setStatus(result.entity().status);
        responseDto.setGraph(graphForPersistence);
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
