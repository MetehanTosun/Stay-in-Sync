package de.unistuttgart.stayinsync.syncnode.logik_engine.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.syncnode.logik_engine.database.DTOs.GraphDTO;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.IOException;
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
    GraphMapperService mapper;

    @Inject
    ObjectMapper jsonObjectMapper;

    @Inject
    GraphCompilerService graphCompilerService;

    @Inject
    GraphValidatorService graphValidator;

    /**
     * Validates and persists a new logic graph to the database. The graph name must be unique.
     *
     * @param name  The unique name for the graph.
     * @param nodes The list of {@link Node} objects representing the graph.
     * @return The persisted {@link LogicGraphEntity}.
     * @throws IllegalArgumentException if the graph is invalid or a graph with the given name already exists.
     */
    public LogicGraphEntity persistGraph(String name, List<Node> nodes) {
        Log.debugf("Validating and persisting new graph with name: %s", name);

        graphValidator.validateGraph(nodes);

        if (LogicGraphEntity.find("name", name).firstResultOptional().isPresent()) {
            throw new IllegalArgumentException("A graph with the name '" + name + "' already exists.");
        }

        GraphDTO graphDto = mapper.graphToDto(nodes);
        try {
            String json = jsonObjectMapper.writeValueAsString(graphDto);

            LogicGraphEntity entity = new LogicGraphEntity();
            entity.name = name;
            entity.graphDefinitionJson = json;
            entity.persist();
            return entity;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize graph '" + name + "' to JSON.", e);
        }
    }

    /**
     * Validates and updates the definition of an existing logic graph, identified by its ID.
     *
     * @param id    The ID of the graph to replace.
     * @param nodes The new list of {@link Node} objects for the graph.
     * @return An {@link Optional} with the updated entity, or empty if no graph with the ID was found.
     * @throws IllegalArgumentException if the new graph structure is invalid.
     */
    public Optional<LogicGraphEntity> updateGraph(Long id, List<Node> nodes) {
        Log.debugf("Validating and replacing graph with id: %d", id);

        graphValidator.validateGraph(nodes);

        Optional<LogicGraphEntity> entityOptional = LogicGraphEntity.findByIdOptional(id);

        if (entityOptional.isPresent()) {
            LogicGraphEntity entity = entityOptional.get();
            GraphDTO graphDto = mapper.graphToDto(nodes);
            try {
                entity.graphDefinitionJson = jsonObjectMapper.writeValueAsString(graphDto);
                return Optional.of(entity);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize graph with id " + id + " to JSON.", e);
            }
        }
        return Optional.empty();
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
    public Optional<List<Node>> findGraphById(Long id) {
        Log.debugf("Loading graph with id: %d", id);

        Optional<LogicGraphEntity> entityOptional = findEntityById(id);
        if (entityOptional.isPresent()) {
            List<Node> hydratedGraph = hydrateGraph(entityOptional.get());
            return Optional.of(hydratedGraph);
        }
        return Optional.empty();
    }

    /**
     * Loads all logic graphs from the database and prepares them for execution.
     *
     * @return A list containing all executable logic graphs.
     */
    @Transactional(SUPPORTS)
    public List<List<Node>> findAllGraphs() {
        Log.debug("Loading all graphs");

        List<LogicGraphEntity> allEntities = LogicGraphEntity.listAll();
        List<List<Node>> allGraphs = new ArrayList<>();

        for (LogicGraphEntity entity : allEntities) {
            try {
                allGraphs.add(hydrateGraph(entity));
            } catch (Exception e) {
                Log.errorf(e, "Skipping graph '%s' (id: %d) due to a loading error.", entity.name, entity.id);
            }
        }

        return allGraphs;
    }

    /**
     * A private helper method to "hydrate" a graph entity into an executable list of nodes.
     * It centralizes the mapping and compiling logic for loading a graph and assumes
     * the graph is already valid as it was validated upon saving.
     */
    private List<Node> hydrateGraph(LogicGraphEntity entity) {
        try {
            GraphDTO dto = jsonObjectMapper.readValue(entity.graphDefinitionJson, GraphDTO.class);
            List<Node> rawGraph = mapper.toNodeGraph(dto);
            // The validator is no longer called here.
            return graphCompilerService.compile(rawGraph);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize graph '" + entity.name + "' from JSON.", e);
        }
    }
}