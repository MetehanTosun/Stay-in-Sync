package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.dto.transformationrule.GraphDTO;
import de.unistuttgart.graphengine.dto.transformationrule.InputDTO;
import de.unistuttgart.graphengine.dto.transformationrule.NodeDTO;
import de.unistuttgart.graphengine.dto.transformationrule.TransformationRulePayloadDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowGraphDTO;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.service.GraphMapper;
import de.unistuttgart.graphengine.service.GraphValidatorService;
import de.unistuttgart.graphengine.validation_error.GraphStatus;
import de.unistuttgart.graphengine.validation_error.ValidationError;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException; // Import der korrekten Exception
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
            // FinalNode
            NodeDTO finalNodeDto = new NodeDTO();
            finalNodeDto.setId(0);
            finalNodeDto.setName("Final Result");
            finalNodeDto.setNodeType("FINAL");
            finalNodeDto.setOffsetX(400);
            finalNodeDto.setOffsetY(150);

            // ConfigNode
            NodeDTO configNodeDto = new NodeDTO();
            configNodeDto.setId(1);
            configNodeDto.setName("Configuration");
            configNodeDto.setNodeType("CONFIG");
            configNodeDto.setOffsetY(150);
            configNodeDto.setChangeDetectionMode("OR");
            configNodeDto.setInputTypes(List.of("ANY"));
            configNodeDto.setOutputType("BOOLEAN");

            InputDTO initialEdge = new InputDTO();
            initialEdge.setId(1);
            initialEdge.setOrderIndex(0);
            finalNodeDto.setInputNodes(List.of(initialEdge));

            GraphDTO defaultGraphDto = new GraphDTO();
            List<NodeDTO> nodes = new ArrayList<>();
            nodes.add(finalNodeDto);
            nodes.add(configNodeDto);
            defaultGraphDto.setNodes(nodes);

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

        TransformationRule ruleToUpdate = storageService.findRuleById(id);
        if (ruleToUpdate == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND,
                    "Rule Not Found", 
                    String.format("Rule ID %d was not found", id));
        }

        ruleToUpdate.name = dto.getName();
        ruleToUpdate.description = dto.getDescription();

        Log.infof("Successfully updated metadata for rule with id %d.", id);
        return ruleToUpdate;
    }

    /**
     * Updates the graph of an existing TransformationRule.
     * This method will now always persist the graph, even if it's invalid.
     * It combines errors from the mapping process (structural errors) and the validation
     * process (logical errors) into a single list.
     */
    @Transactional
    public GraphStorageService.PersistenceResult updateRuleGraph(Long id, VFlowGraphDTO vflowDto) {
        Log.debugf("Updating graph with ruleId: %d", id);

        TransformationRule ruleToUpdate = storageService.findRuleById(id);
        if (ruleToUpdate == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND,
                    "Rule Not Found", 
                    String.format("Rule ID %d was not found", id));
        }
        try {
            GraphDTO graphDto = mapper.vflowToGraphDto(vflowDto);

            GraphMapper.MappingResult mappingResult = mapper.toNodeGraph(graphDto);
            List<Node> successfullyMappedNodes = mappingResult.nodes();
            List<ValidationError> allErrors = new ArrayList<>(mappingResult.mappingErrors());

            int originalNodeCount = vflowDto.getNodes() != null ? vflowDto.getNodes().size() : 0;
            List<ValidationError> validationErrors = validator.validateGraph(successfullyMappedNodes, originalNodeCount);
            allErrors.addAll(validationErrors);

            GraphStatus status = allErrors.isEmpty() ? GraphStatus.FINALIZED : GraphStatus.DRAFT;

            ruleToUpdate.graphStatus = status;
            ruleToUpdate.graph.graphDefinitionJson = jsonObjectMapper.writeValueAsString(graphDto);

            if (!allErrors.isEmpty()) {
                ruleToUpdate.validationErrorsJson = jsonObjectMapper.writeValueAsString(allErrors);
            } else {
                ruleToUpdate.validationErrorsJson = null;
            }

            Log.infof("Successfully updated graph for rule '%s' (id: %d). New status: %s. Total errors: %d",
                    ruleToUpdate.name, id, status, allErrors.size());

            return new GraphStorageService.PersistenceResult(ruleToUpdate, allErrors);

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
        TransformationRule entity = storageService.findRuleById(id);

        List<ValidationError> errors = new ArrayList<>();

        if (entity.graphStatus == GraphStatus.DRAFT && entity.validationErrorsJson != null) {
            try {
                errors = jsonObjectMapper.readValue(entity.validationErrorsJson, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                Log.errorf(e, "Failed to parse validation errors for entity id %d", entity.id);
            }
        }

        return errors;
    }
}