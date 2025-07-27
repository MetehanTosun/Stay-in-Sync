package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.ValidationResult;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphMapper;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.ValidationError;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
public class GraphStorageService {

    @Inject
    GraphMapper mapper;

    @Inject
    ObjectMapper jsonObjectMapper;

    @Inject
    GraphCompilerService graphCompilerService;

    /**
     * A record to hold the result of a graph persistence operation.
     * @param entity The persisted database entity.
     * @param validationErrors A list of all found validation errors. Empty if the graph is valid.
     */
    public record PersistenceResult(TransformationRule entity, List<ValidationError> validationErrors) {}

    /**
     * Persists a fully prepared TransformationRule entity.
     * This method only handles the database interaction.
     *
     * @param ruleEntity The entity to persist.
     */
    @Transactional
    public void persistRule(TransformationRule ruleEntity) {
        ruleEntity.persist();
    }

    /**
     * Finds a TransformationRule entity by its ID.
     *
     * @param id The ID of the rule.
     * @return An Optional containing the found entity.
     */
    @Transactional(SUPPORTS)
    public Optional<TransformationRule> findRuleById(Long id) {
        return TransformationRule.findByIdOptional(id);
    }

    /**
     * Deletes a TransformationRule by its ID.
     *
     * @param id The ID of the rule to delete.
     * @return true if the entity was deleted.
     */
    @Transactional
    public boolean deleteRuleById(Long id) {
        return TransformationRule.deleteById(id);
    }

    /**
     * Retrieves all TransformationRule entities from the database.
     *
     * @return A list of all TransformationRule entities.
     */
    @Transactional(SUPPORTS)
    public List<TransformationRule> findAllRules() {
        return TransformationRule.listAll();
    }

    /**
     * A private helper method to "hydrate" a graph entity into an executable list of nodes.
     * It centralizes the mapping and compiling logic for loading a graph.
     */
    private List<Node> hydrateGraph(LogicGraphEntity entity) {
        try {
            GraphDTO dto = jsonObjectMapper.readValue(entity.graphDefinitionJson, GraphDTO.class);

            List<Node> rawGraph = mapper.toNodeGraph(dto);

            List<Node> compiledGraph = graphCompilerService.compile(rawGraph);

            return compiledGraph;

        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize graph from JSON.", e);
        }
    }

}


