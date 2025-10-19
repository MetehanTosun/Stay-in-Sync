package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.graphengine.dto.transformationrule.GraphPersistenceResponseDTO;
import de.unistuttgart.graphengine.dto.transformationrule.TransformationRulePayloadDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowGraphDTO;
import de.unistuttgart.graphengine.dto.vFlow.VflowGraphResponseDTO;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.OperatorMetadataDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationRuleDTO;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.TransformationRuleMapperService;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.TransformationRuleService;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.GraphStorageService;
import de.unistuttgart.stayinsync.core.configuration.util.OperatorMetadataService;
import de.unistuttgart.graphengine.validation_error.ValidationError;
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
import java.util.Optional;
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
    OperatorMetadataService operatorMetadataService;


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
    public Response createTransformationRule(TransformationRulePayloadDTO payload, @Context UriInfo uriInfo) {
        if (payload.getName() == null || payload.getName().trim().isEmpty()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST,"Invalid Request",
                    "Rule name must not be empty");
        }
        try {
            GraphStorageService.PersistenceResult result = ruleService.createRule(payload);
            TransformationRule createdRule = result.entity();
            var location = uriInfo.getAbsolutePathBuilder().path(Long.toString(createdRule.id)).build();
            Log.infof("Successfully created TransformationRule '%s' with id %d.", createdRule.name, createdRule.id);
            return Response.created(location).entity(ruleMapper.toRuleDTO(createdRule)).build();
        } catch (CoreManagementException e) {
            Log.warnf("Failed to create transformation rule: %s", e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "Unexpected error in createTransformationRule for name '%s'", payload.getName());
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Creation Failed", "An unexpected error occurred while creating the rule.");
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates the metadata of an existing Transformation Rule")
    public Response updateTransformationRuleMetadata(@Parameter(name = "id", required = true) @PathParam("id") Long id, TransformationRulePayloadDTO payload) {
        try {
            TransformationRule updatedRule = ruleService.updateRuleMetadata(id, payload);
            Log.infof("Successfully updated metadata for rule with id %d.", id);
            return Response.ok(ruleMapper.toRuleDTO(updatedRule)).build();
        } catch (Exception e) {
            Log.errorf(e, "Unexpected error in updateTransformationRuleMetadata for id %d", id);
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Update Failed", "An unexpected error occurred while updating the rule metadata.");
        }
    }

    @PUT
    @Path("/{id}/graph")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates the graph of an existing Transformation Rule")
    public Response updateTransformationRuleGraph(@Parameter(name = "id", required = true) @PathParam("id") Long id, VFlowGraphDTO vflowDto) {
        try {
            var result = ruleService.updateRuleGraph(id, vflowDto);

            GraphPersistenceResponseDTO responseDto = new GraphPersistenceResponseDTO(
                    ruleMapper.toGraphDTO(result.entity()),
                    result.validationErrors()
            );

            Log.infof("Successfully updated graph for rule with id %d. New status: %s", id, result.entity().graphStatus);
            return Response.ok(responseDto).build();

        } catch (Exception e) {
            Log.errorf(e, "Unexpected error in updateTransformationRuleGraph for id %d", id);
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Update Failed", "An unexpected error occurred while updating the graph.");
        }
    }



    @GET
    @Path("/{id}")
    @Operation(summary = "Returns the metadata for a single Transformation Rule")
    public TransformationRuleDTO getTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return ruleMapper.toRuleDTO(graphStorage.findRuleById(id));
    }

    /**
     * Retrieves the metadata for all Transformation Rules.
     *
     * @return A list of {@link TransformationRuleDTO}s.
     */
    @GET
    @Operation(summary = "Returns the metadata for all Transformation Rules")
    public List<TransformationRuleDTO> getAllTransformationRules(
            @Parameter(name = "unassigned", description = "An optional filter receive only unassiged rules") @QueryParam("unassigned") Optional<Boolean> unassigned
    ) {
        List<TransformationRule> rules = graphStorage.findAllRules();
        if(unassigned.isPresent())
        {
            rules = rules.stream()
                    .filter(rule -> unassigned.get() ? rule.transformation == null : rule.transformation != null)
                    .collect(Collectors.toList());
        }

        Log.infof("Found %d transformation rules.", rules.size());
        return rules.stream()
                .map(ruleMapper::toRuleDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}/graph")
    @Operation(summary = "Returns the VFlow graph definition and validation errors for a single rule")
    public VflowGraphResponseDTO getGraphForTransformationRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {

        TransformationRule entity = graphStorage.findRuleById(id);

        VFlowGraphDTO graphData = ruleMapper.toVFlowDto(entity);
        List<ValidationError> errors = ruleService.getValidationErrorsForRule(id);

        Log.infof("Returning graph for rule '%s' with %d validation errors.", entity.name, errors.size());
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
            Log.infof("Successfully deleted TransformationRule with id %d", id);
            return Response.noContent().build();
        } else {
            Log.warnf("Attempted to delete non-existent transformation rule with id %d", id);
            throw new CoreManagementException(Response.Status.NOT_FOUND,
                    "Not Found", "TransformationRule with id %d could not be found for deletion.", id);
        }
    }
}
