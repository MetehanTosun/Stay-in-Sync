package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.service.GraphMapper;
import de.unistuttgart.graphengine.validation_error.ValidationError;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
public class GraphStorageService {

    /**
     * A record to hold the result of a graph persistence operation.
     *
     * @param entity           The persisted database entity.
     * @param validationErrors A list of all found validation errors. Empty if the graph is valid.
     */
    public record PersistenceResult(TransformationRule entity, List<ValidationError> validationErrors) {
    }

    /**
     * Persists a fully prepared TransformationRule entity.
     * This method only handles the database interaction.
     *
     * @param ruleEntity The entity to persist.
     * @throws CoreManagementException If the database persistence operation fails.
     */
    @Transactional
    public void persistRule(TransformationRule ruleEntity) {
        Log.debugf("Persisting entity with name: '%s'", ruleEntity.name);
        try {
            ruleEntity.persist();
            Log.infof("Successfully persisted TransformationRule '%s' with id %d.", ruleEntity.name, ruleEntity.id);
        } catch (Exception e) {
            Log.errorf(e, "Database error while persisting TransformationRule '%s'", ruleEntity.name);
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR, "Database Error", "Could not persist rule.", e);
        }
    }

    /**
     * Finds a TransformationRule entity by its ID.
     *
     * @param id The ID of the rule.
     * @return An Optional containing the found entity.
     */
    @Transactional(SUPPORTS)
    public TransformationRule findRuleById(Long id) {
        Log.debugf("Finding TransformationRule entity with id: %d", id);
        TransformationRule rule = TransformationRule.findById(id);
        if (rule == null) {
            throw new CoreManagementException(Response.Status.NOT_FOUND, "Transfromation Rule was not found", "There is no transformation rule with id %d", id);
        }
        return rule;
    }

    /**
     * Finds a TransformationRule entity by its unique name.
     *
     * @param name The unique name of the rule.
     * @return An Optional containing the found entity.
     */
    @Transactional(SUPPORTS)
    public Optional<TransformationRule> findRuleByName(String name) {
        Log.debugf("Finding TransformationRule entity by name = %s", name);
        return TransformationRule.find("name", name).firstResultOptional();
    }

    /**
     * Deletes a TransformationRule by its ID.
     *
     * @param id The ID of the rule to delete.
     * @return true if the entity was deleted.
     */
    @Transactional
    public boolean deleteRuleById(Long id) {
        Log.debugf("Deleting rule with id: %d", id);
        return TransformationRule.deleteById(id);
    }

    /**
     * Retrieves all TransformationRule entities from the database.
     *
     * @return A list of all TransformationRule entities.
     */
    @Transactional(SUPPORTS)
    public List<TransformationRule> findAllRules() {
        Log.debug("Finding all TransformationRules.");
        return TransformationRule.listAll();
    }
}