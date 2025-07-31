package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException; // Import der korrekten Exception
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.NodeDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.TransformationRulePayloadDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VFlowGraphDTO;
import de.unistuttgart.stayinsync.transport.exception.NodeConfigurationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphMapper;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.ValidationError;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static jakarta.transaction.Transactional.TxType.SUPPORTS;

/**
 * The central service for managing Transformation Rules.
 * It orchestrates the tasks of mapping, validating, and persisting graph data.
 */
@ApplicationScoped
public class TransformationRuleService {

    @Inject
    GraphMapper mapper;
    @Inject
    GraphValidatorService validator;
    @Inject
    GraphStorageService storageService;
    @Inject
    ObjectMapper jsonObjectMapper;

    /**
     * Creates a new TransformationRule with a default graph.
     */
    @Transactional
    public GraphStorageService.PersistenceResult createRule(TransformationRulePayloadDTO payload) {
        Log.debugf("Creating a new rule with name: %s", payload.getName());

        if (storageService.findRuleByName(payload.getName()).isPresent()) {
            throw new CoreManagementException(Response.Status.CONFLICT,
                    "Duplicate Name",
                    "A TransformationRule with the name '%s' already exists.", payload.getName());
        }

        try {
            NodeDTO finalNodeDto = new NodeDTO();
            finalNodeDto.setId(0);
            finalNodeDto.setName("Final Result");
            finalNodeDto.setNodeType("FINAL");

            GraphDTO defaultGraphDto = new GraphDTO();
            defaultGraphDto.setNodes(Collections.singletonList(finalNodeDto));

            TransformationRule rule = new TransformationRule();
            rule.name = payload.getName();
            rule.description = payload.getDescription();
            rule.graphStatus = GraphStatus.FINALIZED;

            LogicGraphEntity graphEntity = new LogicGraphEntity();
            graphEntity.graphDefinitionJson = jsonObjectMapper.writeValueAsString(defaultGraphDto);
            rule.graph = graphEntity;

            storageService.persistRule(rule);

            Log.infof("Successfully created new rule '%s' with id %d.", rule.name, rule.id);
            rule.graph = graphEntity;
            return new GraphStorageService.PersistenceResult(rule, new ArrayList<>());

        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialize default graph for new rule '%s'", payload.getName());
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Serialization Error", "Failed to create default graph.", e);
        }
    }

    /**
     * Updates the metadata of an existing TransformationRule.
     */
    @Transactional
    public TransformationRule updateRuleMetadata(Long id, TransformationRulePayloadDTO dto) {
        Log.debugf("Updating rule metadata with id: %d", id);

        TransformationRule ruleToUpdate = storageService.findRuleById(id)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND,
                        "Not Found", "Rule with id %d not found.", id));

        ruleToUpdate.name = dto.getName();
        ruleToUpdate.description = dto.getDescription();

        Log.infof("Successfully updated metadata for rule with id %d.", id);
        return ruleToUpdate;
    }

    /**
     * Updates the graph of an existing TransformationRule.
     */
    @Transactional
    public GraphStorageService.PersistenceResult updateRuleGraph(Long id, VFlowGraphDTO vflowDto) {
        Log.debugf("Updating graph with ruleId: %d", id);

        TransformationRule ruleToUpdate = storageService.findRuleById(id)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND,
                        "Not Found", "Rule with id %d not found.", id));

        try {
            GraphDTO graphDto = mapper.vflowToGraphDto(vflowDto);
            List<Node> nodeGraph = mapper.toNodeGraph(graphDto);

            List<ValidationError> validationErrors = validator.validateGraph(nodeGraph);
            GraphStatus status = validationErrors.isEmpty() ? GraphStatus.FINALIZED : GraphStatus.DRAFT;

            ruleToUpdate.graphStatus = status;
            ruleToUpdate.graph.graphDefinitionJson = jsonObjectMapper.writeValueAsString(graphDto);

            if (!validationErrors.isEmpty()) {
                ruleToUpdate.validationErrorsJson = jsonObjectMapper.writeValueAsString(validationErrors);
            } else {
                ruleToUpdate.validationErrorsJson = null; // Clear old errors if the graph is now valid.
            }

            Log.infof("Successfully updated graph for rule '%s' (id: %d). New status: %s",
                    ruleToUpdate.name, id, status);

            return new GraphStorageService.PersistenceResult(ruleToUpdate, validationErrors);

        } catch (NodeConfigurationException e) {
            Log.warnf(e, "Client sent a malformed graph for rule id %d: %s", id, e.getMessage());
            throw new CoreManagementException(Response.Status.BAD_REQUEST,
                    "Invalid node configuration", e.getMessage(), e);

        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialize graph or errors for rule with id %d", id);
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Serialization Error", "Failed to serialize graph for update.", e);
        }
    }

    /**
     * Retrieves the list of validation errors for a specific rule.
     *
     * @param id The ID of the rule.
     * @return A list of {@link ValidationError} objects. The list is empty if the rule is finalized or has no errors.
     */
    @Transactional(SUPPORTS)
    public List<ValidationError> getValidationErrorsForRule(Long id) {
        TransformationRule entity = storageService.findRuleById(id)
                .orElseThrow(() -> new NotFoundException("TransformationRule with id " + id + " not found."));

        List<ValidationError> errors = new ArrayList<>();

        if (entity.graphStatus == GraphStatus.DRAFT && entity.validationErrorsJson != null) {
            try {
                errors = jsonObjectMapper.readValue(entity.validationErrorsJson, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                Log.errorf(e, "Failed to parse validation errors for entity id %d", entity.id);

                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("!!! JSON PROCESSING EXCEPTION GEWORFEN !!!");
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                e.printStackTrace();

            }
        }

        return errors;
    }
}