package de.unistuttgart.stayinsync.core.configuration.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.OperatorMetadataDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationRuleDTO;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.TransformationRuleMapperService;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.TransformationRuleService;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.GraphStorageService;
import de.unistuttgart.stayinsync.core.configuration.util.OperatorMetadata;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphPersistenceResponseDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.TransformationRulePayloadDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VFlowGraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VflowGraphResponseDTO;
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

import java.util.List;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/transformation-rule")
@Produces(APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Transformation Rule", description = "Endpoints for managing logic graph transformation rules")
public class TransformationRuleResource {


    @Inject
    TransformationRuleService ruleService;

    @Inject
    TransformationRuleMapperService ruleMapper;

    @Inject
    GraphStorageService graphStorage;

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
    @Operation(summary = "Creates a new Transformation Rule shell",
            description = "Creates the initial rule with metadata and a default graph containing only a FinalNode.")
    public Response createTransformationRule(TransformationRulePayloadDTO dto, @Context UriInfo uriInfo) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BadRequestException("Rule name must not be empty.");
        }

        GraphStorageService.PersistenceResult result = ruleService.createRule(dto);

        TransformationRule createdRule = result.entity();

        var location = uriInfo.getAbsolutePathBuilder().path(Long.toString(createdRule.id)).build();
        Log.debugf("New TransformationRule created with URI %s", location);

        return Response.created(location).entity(ruleMapper.toRuleDTO(createdRule)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates the metadata of an existing Transformation Rule")
    public Response updateTransformationRuleMetadata(@Parameter(name = "id", required = true) @PathParam("id") Long id, TransformationRulePayloadDTO dto) { // Wir verwenden hier das PayloadDTO, da es name und beschreibung enthält

        TransformationRule updatedRule = ruleService.updateRuleMetadata(id, dto);

        Log.debugf("Metadata for TransformationRule with id %d was updated.", id);

        return Response.ok(ruleMapper.toRuleDTO(updatedRule)).build();
    }

    @PUT
    @Path("/{id}/graph")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates the graph of an existing Transformation Rule")
    public Response updateTransformationRuleGraph(@Parameter(name = "id", required = true) @PathParam("id") Long id, VFlowGraphDTO vflowDto) {

        var result = ruleService.updateRuleGraph(id, vflowDto);

        GraphPersistenceResponseDTO responseDto = new GraphPersistenceResponseDTO(
                ruleMapper.toGraphDTO(result.entity()),
                result.validationErrors()
        );

        Log.debugf("Graph for TransformationRule with id %d was updated.", id);
        return Response.ok(responseDto).build();
    }



    @GET
    @Path("/{id}")
    @Operation(summary = "Returns the metadata for a single Transformation Rule")
    public TransformationRuleDTO getTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return graphStorage.findRuleById(id)
                .map(ruleMapper::toRuleDTO)
                .orElseThrow(() -> new NotFoundException("TransformationRule with id " + id + " not found."));
    }

    /**
     * Retrieves the metadata for all Transformation Rules.
     *
     * @return A list of {@link TransformationRuleDTO}s.
     */
    @GET
    @Operation(summary = "Returns the metadata for all Transformation Rules")
    public List<TransformationRuleDTO> getAllTransformationRules() {
        return graphStorage.findAllRules().stream()
                .map(ruleMapper::toRuleDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}/graph")
    @Operation(summary = "Returns the VFlow graph definition and validation errors for a single rule")
    public VflowGraphResponseDTO getGraphForTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {

        TransformationRule entity = graphStorage.findRuleById(id)
                .orElseThrow(() -> new NotFoundException("TransformationRule with id " + id + " not found."));

        VFlowGraphDTO graphData = ruleMapper.toVFlowDto(entity);

        List<ValidationError> errors = ruleService.getValidationErrorsForRule(id);

        return new VflowGraphResponseDTO(graphData.getNodes(), graphData.getEdges(), errors);
    }

    /**
     * Deletes an existing TransformationRule and its associated graph.
     *
     * @param id The ID of the rule to delete.
     * @return A Response with status 204 (No Content) on success.
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes an existing Transformation Rule")
    public Response deleteTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        boolean deleted = graphStorage.deleteRuleById(id);
        if (deleted) {
            Log.debugf("TransformationRule with id %d deleted.", id);
            return Response.noContent().build();
        } else {
            throw new NotFoundException("TransformationRule with id " + id + " not found for deletion.");
        }
    }
}
