package de.unistuttgart.stayinsync.syncnode.logik_engine.Database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.syncnode.logik_engine.Database.DTOs.GraphDefinitionDTO;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode;
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

    /**
     * Saves a new logic graph to the database. The graph name must be unique.
     * If a graph with the same name already exists, an exception will be thrown.
     *
     * @param name  The unique name for the graph.
     * @param nodes The list of {@link LogicNode} objects representing the graph.
     * @throws IllegalArgumentException if a graph with the given name already exists.
     */
    @Transactional
    public void saveGraph(String name, List<LogicNode> nodes) {
        // Check if graph with this name already exists
        Optional<LogicGraphEntity> existingEntity = LogicGraphEntity.<LogicGraphEntity>find("name", name).firstResultOptional();
        if (existingEntity.isPresent()) {
            throw new IllegalArgumentException("A graph with the name '" + name + "' already exists. Please choose a different name.");
        }

        GraphDefinitionDTO graphDto = mapper.graphToDto(nodes);
        try {
            String json = jsonObjectMapper.writeValueAsString(graphDto);

            // Create new entity
            LogicGraphEntity entity = new LogicGraphEntity();
            entity.name = name;
            entity.graphDefinitionJson = json;
            entity.persist();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize graph '" + name + "' to JSON.", e);
        }
    }

    /**
     * Loads a logic graph from the database by its unique name.
     *
     * @param name The name of the graph to load.
     * @return An {@link Optional} containing the list of {@link LogicNode}s if found, otherwise empty.
     */
    public Optional<List<LogicNode>> loadGraph(String name) {
        // Find the graph in database
        Optional<LogicGraphEntity> entityOptional = LogicGraphEntity.<LogicGraphEntity>find("name", name).firstResultOptional();

        // If not found, return empty
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }

        // Get the entity
        LogicGraphEntity entity = entityOptional.get();

        try {
            // Convert JSON back to DTO
            GraphDefinitionDTO dto = jsonObjectMapper.readValue(entity.graphDefinitionJson, GraphDefinitionDTO.class);

            // Convert DTO back to LogicNodes
            List<LogicNode> nodes = mapper.toLogicNode(dto);

            return Optional.of(nodes);

        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize graph '" + name + "' from JSON.", e);
        }
    }
}
