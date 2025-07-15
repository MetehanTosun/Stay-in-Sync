package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.ValidationResult;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

/**
 * Provides an API to save, load, and manage logic graphs in the database.
 */
@ApplicationScoped
@Transactional(REQUIRED) // Default transaction type for write operations
public class GraphStorageService {

    @Inject
    GraphMapper mapper;

    @Inject
    GraphValidatorService validator;

    @Inject
    ObjectMapper jsonObjectMapper;

    public record PersistenceResult(LogicGraphEntity entity, ValidationResult validationResult) {}

    /**
     * Maps, validates, and persists a new logic graph to the database.
     * <p>
     * This method orchestrates the entire process. It validates the graph and sets the
     * {@code finalized} flag based on the outcome before saving the entity. It does
     * not throw an exception for validation failures, as saving an un-finalized graph
     * is a valid operation.
     *
     * @param graphDto The DTO containing the full graph definition.
     * @return The persisted {@link LogicGraphEntity}, including its finalization status.
     * @throws IllegalArgumentException if a graph with the given name already exists.
     */
    @Transactional
    public PersistenceResult persistGraph(GraphDTO graphDto) {
        Log.debugf("Validating and persisting new graph with name: %s", graphDto.getName());

        if (LogicGraphEntity.find("name", graphDto.getName()).firstResultOptional().isPresent()) {
            throw new IllegalArgumentException("A graph with the name '" + graphDto.getName() + "' already exists.");
        }

        List<Node> nodeGraph = mapper.toNodeGraph(graphDto);

        ValidationResult validationResult = validator.validateGraph(nodeGraph);

        boolean isFinalized = validationResult.isValid();

        try {
            String json = jsonObjectMapper.writeValueAsString(graphDto);

            LogicGraphEntity entity = new LogicGraphEntity();
            entity.name = graphDto.getName();
            entity.graphDefinitionJson = json;
            entity.finalized = isFinalized;
            entity.persist();

            // Log a warning if the graph was saved as a non-finalized draft.
            if (!isFinalized) {
                Log.warnf("Graph '%s' (id: %d) was saved as a non-finalized draft due to validation errors: %s",
                        graphDto.getName(), entity.id, validationResult.errorMessages());
            }

            return new PersistenceResult(entity, validationResult);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize graph '" + graphDto.getName() + "' to JSON.", e);
        }
    }

    /**
     * Maps, validates, and updates an existing logic graph in the database.
     * <p>
     * This method orchestrates the entire update process. It finds the existing entity,
     * validates the new graph structure provided in the DTO, and then updates the
     * entity with the new definition and a `finalized` flag based on the validation outcome.
     *
     * @param id       The ID of the graph to update.
     * @param graphDTO The DTO containing the new, complete graph definition.
     * @return A {@link PersistenceResult} containing the updated entity and the validation details.
     * @throws NotFoundException if no graph with the given ID is found.
     */
    public PersistenceResult updateGraph(Long id, GraphDTO graphDTO) {
        Log.debugf("Validating and replacing graph with id: %d", id);
        LogicGraphEntity entity = (LogicGraphEntity) LogicGraphEntity.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Graph with id " + id + " not found."));

        List<Node> nodeGraph = mapper.toNodeGraph(graphDTO);
        ValidationResult validationResult = validator.validateGraph(nodeGraph);
        boolean isFinalized = validationResult.isValid();

        try {
            entity.name = graphDTO.getName();
            entity.graphDefinitionJson = jsonObjectMapper.writeValueAsString(graphDTO);
            entity.finalized = isFinalized;
            entity.persist();

            return new PersistenceResult(entity, validationResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize graph with id " + id + " to JSON.", e);
        }
    }

    /**
     * Deletes a logic graph from the database by its ID.
     *
     * @param id The ID of the graph to delete.
     * @return
     */
    public boolean deleteGraphById(Long id) {
        Log.debugf("Deleting graph with id: %d", id);
        return LogicGraphEntity.deleteById(id);
    }

    /**
     * Retrieves the names of all existing logic graphs.
     *
     * @return A list of all graph names.
     */
    @Transactional(SUPPORTS)
    public List<String> findAllGraphNames() {
        Log.debug("Finding all graph names");

        List<LogicGraphEntity> allEntities = LogicGraphEntity.listAll();
        List<String> graphNames = new ArrayList<>();

        for (LogicGraphEntity entity : allEntities) {
            graphNames.add(entity.name);
        }

        return graphNames;
    }

    /**
     * Finds a single graph entity by its ID.
     *
     * @param id The ID of the graph.
     * @return An {@link Optional} containing the {@link LogicGraphEntity} if found.
     */
    @Transactional(SUPPORTS)
    public Optional<LogicGraphEntity> findEntityById(Long id) {
        Log.debugf("Finding graph entity by id = %d", id);
        return LogicGraphEntity.findByIdOptional(id);
    }

    /**
     * Loads a single logic graph by its ID and prepares it for execution.
     *
     * @param id The ID of the graph to load.
     * @return An {@link Optional} containing the executable graph as a {@code List<Node>}, if found.
     */
    @Transactional(SUPPORTS)
    public Optional<LogicGraphEntity> findGraphById(Long id) {
        Log.debugf("Loading graph with id: %d", id);

        return findEntityById(id);
    }

    /**
     * Loads all logic graphs from the database and prepares them for execution.
     *
     * @return A list containing all executable logic graphs.
     */
    @Transactional(SUPPORTS)
    public List<LogicGraphEntity> findAllGraphs() {
        Log.debug("Loading all graphs");

        return LogicGraphEntity.listAll();
    }

//    /**
//     * A private helper method to "hydrate" a graph entity into an executable list of nodes.
//     * It centralizes the mapping and compiling logic for loading a graph and assumes
//     * the graph is already valid as it was validated upon saving.
//     */
//    private List<Node> hydrateGraph(LogicGraphEntity entity) {
//        try {
//            GraphDTO dto = jsonObjectMapper.readValue(entity.graphDefinitionJson, GraphDTO.class);
//            List<Node> rawGraph = mapper.toNodeGraph(dto);
//            // The validator is no longer called here.
//            return graphCompilerService.compile(rawGraph);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to deserialize graph '" + entity.name + "' from JSON.", e);
//        }
//    }
}