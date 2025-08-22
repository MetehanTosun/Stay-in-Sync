package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;

import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.NodeDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.TransformationRulePayloadDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VFlowGraphDTO;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.ValidationError;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * The default graph contains only a single FinalNode, which defaults to 'true'.
     * The rule is immediately marked as FINALIZED.
     *
     * @param dto The DTO containing the metadata for the new rule.
     * @return A PersistenceResult containing the newly created entity and an empty validation list.
     */
    @Transactional
    public GraphStorageService.PersistenceResult createRule(TransformationRulePayloadDTO dto) {
        Log.debugf("Creating new rule with name: %s", dto.getName());

        // 1. Create the default graph structure.
        NodeDTO finalNodeDto = new NodeDTO();
        finalNodeDto.setId(0);
        finalNodeDto.setName("Final Result");
        finalNodeDto.setNodeType("FINAL");

        GraphDTO defaultGraphDto = new GraphDTO();
        defaultGraphDto.setNodes(Collections.singletonList(finalNodeDto));

        // 2. Create the database entities.
        TransformationRule rule = new TransformationRule();
        rule.name = dto.getName();
        rule.description = dto.getDescription();
        rule.graphStatus = GraphStatus.FINALIZED; // A default graph is always valid.

        LogicGraphEntity graphEntity = new LogicGraphEntity();
        try {
            graphEntity.graphDefinitionJson = jsonObjectMapper.writeValueAsString(defaultGraphDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize default graph.", e);
        }

        rule.graph = graphEntity;

        // 3. Persist the new rule.
        storageService.persistRule(rule);

        // 4. Return the result. The validation error list is empty.
        return new GraphStorageService.PersistenceResult(rule, new ArrayList<>());
    }

    /**
     * Updates only the metadata (name and description) of an existing TransformationRule.
     * The graph definition and its status remain unchanged.
     *
     * @param id  The ID of the rule to update.
     * @param dto The DTO containing the new metadata.
     * @return The updated TransformationRule entity.
     * @throws NotFoundException if no rule with the given ID is found.
     */
    @Transactional
    public TransformationRule updateRuleMetadata(Long id, TransformationRulePayloadDTO dto) {
        TransformationRule ruleToUpdate = storageService.findRuleById(id);

        ruleToUpdate.name = dto.getName();
        ruleToUpdate.description = dto.getDescription();

        Log.debugf("Metadata for TransformationRule with id %d was updated.", id);
        return ruleToUpdate;
    }

    /**
     * Updates the graph of an existing TransformationRule.
     * This method orchestrates the entire process of mapping, validating, and persisting the new graph structure.
     *
     * @param id       The ID of the rule whose graph is to be updated.
     * @param vflowDto The DTO from the frontend containing the new graph structure.
     * @return A PersistenceResult object with the updated entity and the validation result.
     */
    @Transactional
    public GraphStorageService.PersistenceResult updateRuleGraph(Long id, VFlowGraphDTO vflowDto) {
        // 1. Find the existing rule entity.
        TransformationRule ruleToUpdate = storageService.findRuleById(id);

        // 2. Translate the VFlow format into the internal persistence format (GraphDTO).
        GraphDTO graphDto = mapper.vflowToGraphDto(vflowDto);

        // 3. Create the "intelligent" domain objects for validation.
        List<Node> nodeGraph = mapper.toNodeGraph(graphDto);

        // 4. Validate the new graph structure.
        List<ValidationError> validationErrors = validator.validateGraph(nodeGraph);
        GraphStatus status = validationErrors.isEmpty() ? GraphStatus.FINALIZED : GraphStatus.DRAFT;

        // 5. Update the entity's properties.
        ruleToUpdate.graphStatus = status;

        try {
            ruleToUpdate.graph.graphDefinitionJson = jsonObjectMapper.writeValueAsString(graphDto);
            if (!validationErrors.isEmpty()) {
                ruleToUpdate.validationErrorsJson = jsonObjectMapper.writeValueAsString(validationErrors);
            } else {
                ruleToUpdate.validationErrorsJson = null; // Clear old errors if the graph is now valid
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize graph or errors to JSON.", e);
        }

        // The entity is managed, so changes will be persisted.
        return new GraphStorageService.PersistenceResult(ruleToUpdate, validationErrors);
    }


    @Transactional
    public List<ValidationError> getValidationErrorsForRule(Long id) {
        TransformationRule entity = storageService.findRuleById(id);

        // ====================== START DEBUGGING ======================
        Log.info("====== PRÜFUNG INNERHALB DER TRANSAKTION ======");
        if (entity.validationErrorsJson == null) {
            Log.info("Feld 'validationErrorsJson' ist hier NULL.");
        } else {
            Log.infof("Feld 'validationErrorsJson' ist NICHT NULL. Länge: %d", entity.validationErrorsJson.length());
            Log.infof("Inhalt: %s", entity.validationErrorsJson);
        }
        Log.info("=================================================");
        // ======================= END DEBUGGING =======================

        List<ValidationError> errors = new ArrayList<>();
        // Füge zur Sicherheit eine Prüfung auf einen leeren String hinzu
        if (entity.graphStatus == GraphStatus.DRAFT && entity.validationErrorsJson != null && !entity.validationErrorsJson.trim().isEmpty()) {
            try {
                errors = jsonObjectMapper.readValue(entity.validationErrorsJson, new TypeReference<>() {});
                Log.info("DESERIALISIERUNG ERFOLGREICH. GEFUNDENE FEHLER: " + errors.size());
            } catch (JsonProcessingException e) {
                Log.error("FEHLER BEI DER DESERIALISIERUNG!", e);
            }
        }
        return errors;
    }
}
