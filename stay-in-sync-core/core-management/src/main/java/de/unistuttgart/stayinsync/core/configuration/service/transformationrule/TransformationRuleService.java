package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;

import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.TransformationRulePayloadDTO;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphMapper;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.ValidationError;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
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
     * Creates a new TransformationRule and its associated graph.
     *
     * @param payload The DTO containing all necessary information from the client.
     * @return A PersistenceResult object with the persisted entity and the validation result.
     */
    @Transactional
    public GraphStorageService.PersistenceResult createRule(TransformationRulePayloadDTO payload) {
        // 1. Translate the frontend VFlow format to the internal persistence format (GraphDTO).
        GraphDTO graphDto = mapper.vflowToGraphDto(payload.getGraph());

        // 2. Create the "intelligent" domain objects for validation.
        List<Node> nodeGraph = mapper.toNodeGraph(graphDto);

        // 3. Validate the graph.
        List<ValidationError> validationErrors = validator.validateGraph(nodeGraph);
        GraphStatus status = validationErrors.isEmpty() ? GraphStatus.FINALIZED : GraphStatus.DRAFT;

        // 4. Create the entities.
        TransformationRule rule = new TransformationRule();
        rule.name = payload.getName();
        rule.description = payload.getDescription();
        rule.graphStatus = status;

        LogicGraphEntity graphEntity = new LogicGraphEntity();

        try {
            graphEntity.graphDefinitionJson = jsonObjectMapper.writeValueAsString(graphDto);
            if (!validationErrors.isEmpty()) {
                rule.validationErrorsJson = jsonObjectMapper.writeValueAsString(validationErrors);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize graph or errors to JSON.", e);
        }

        rule.graph = graphEntity;

        // 5. Pass the prepared entity to the storage service for simple persistence.
        storageService.persistRule(rule);

        return new GraphStorageService.PersistenceResult(rule, validationErrors);
    }

    /**
     * Updates an existing TransformationRule and its associated graph.
     *
     * @param id      The ID of the rule to update.
     * @param payload The DTO containing the new data.
     * @return A PersistenceResult object with the updated entity and the validation result.
     */
    @Transactional
    public GraphStorageService.PersistenceResult updateRule(Long id, TransformationRulePayloadDTO payload) {
        TransformationRule ruleToUpdate = storageService.findRuleById(id)
                .orElseThrow(() -> new NotFoundException("Rule with id " + id + " not found."));

        GraphDTO graphDto = mapper.vflowToGraphDto(payload.getGraph());
        List<Node> nodeGraph = mapper.toNodeGraph(graphDto);
        List<ValidationError> validationErrors = validator.validateGraph(nodeGraph);
        GraphStatus status = validationErrors.isEmpty() ? GraphStatus.FINALIZED : GraphStatus.DRAFT;

        ruleToUpdate.name = payload.getName();
        ruleToUpdate.description = payload.getDescription();
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

        // The entity is managed, so changes will be persisted on transaction commit.
        return new GraphStorageService.PersistenceResult(ruleToUpdate, validationErrors);
    }
    @Transactional // Wichtig!
    public List<ValidationError> getValidationErrorsForRule(Long id) {
        TransformationRule entity = storageService.findRuleById(id)
                .orElseThrow(() -> new NotFoundException("TransformationRule with id " + id + " not found."));

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
