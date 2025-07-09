package de.unistuttgart.stayinsync.syncnode.logik_engine.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.syncnode.logik_engine.database.DTOs.GraphDTO;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Provides an API to save and load logic graphs to and from the database.
 */
@ApplicationScoped
public class GraphStorageService {

    @Inject
    GraphMapper mapper;

    // Quarkus provides a pre-configured ObjectMapper instance via CDI
    @Inject
    ObjectMapper jsonObjectMapper;

    @Inject
    GraphCompilerService graphCompilerService;

    @Inject
    GraphValidatorService graphValidator;

    /**
     * Saves a new logic graph to the database. The graph name must be unique.
     *
     * @param name  The unique name for the graph.
     * @param nodes The list of {@link Node} objects representing the graph.
     * @throws IllegalArgumentException if a graph with the given name already exists.
     */
    @Transactional
    public void saveGraph(String name, List<Node> nodes) {
        Optional<LogicGraphEntity> existingEntity = LogicGraphEntity.find("name", name).firstResultOptional();
        if (existingEntity.isPresent()) {
            throw new IllegalArgumentException("A graph with the name '" + name + "' already exists.");
        }

        // Use the mapper to convert the list of Node objects to the new GraphDTO
        GraphDTO graphDto = mapper.graphToDto(nodes);
        try {
            String json = jsonObjectMapper.writeValueAsString(graphDto);

            LogicGraphEntity entity = new LogicGraphEntity();
            entity.name = name;
            entity.graphDefinitionJson = json;
            entity.persist();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize graph '" + name + "' to JSON.", e);
        }
    }

    /**
     * Loads a logic graph from the database by its name and prepares it for execution.
     *
     * @param name The name of the graph to load.
     * @return An {@link Optional} containing the executable list of {@link Node}s if found.
     */
    public Optional<List<Node>> loadGraph(String name) {
        Optional<LogicGraphEntity> entityOptional = LogicGraphEntity.find("name", name).firstResultOptional();

        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }

        LogicGraphEntity entity = entityOptional.get();

        try {
            // Deserialize JSON into the new GraphDTO
            GraphDTO dto = jsonObjectMapper.readValue(entity.graphDefinitionJson, GraphDTO.class);
            // Use the mapper's new method to get a List<Node>
            List<Node> rawGraph = mapper.toNodeGraph(dto);

            // The compiler and validator now work with List<Node>
            List<Node> compiledGraph = graphCompilerService.compile(rawGraph);
            graphValidator.validateGraph(compiledGraph);

            return Optional.of(compiledGraph);

        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize graph '" + name + "' from JSON.", e);
        }
    }
}
