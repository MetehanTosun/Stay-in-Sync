package de.unistuttgart.stayinsync.syncnode.logik_engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.syncnode.logik_engine.database.GraphStorageService;
import de.unistuttgart.stayinsync.syncnode.logik_engine.database.LogicGraphEntity;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ConstantNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ProviderNode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An integration test for the refactored graph architecture.
 * This test covers the full lifecycle: creating a graph in memory, saving it to
 * the database, loading it back, and verifying its integrity and functionality.
 */
@QuarkusTest
public class GraphPersistenceTest {

    @Inject
    GraphStorageService storageService;

    @Inject
    EntityManager entityManager;

    @Inject
    LogicGraphEvaluator evaluator;


    /**
     * Ensures a clean database state before each test.
     */
    @BeforeEach
    @Transactional
    void cleanupDatabase() {
        LogicGraphEntity.deleteAll();
    }

    @Test
    @Transactional
    public void testSaveLoadAndEvaluateRefactoredGraph() {
        // =================================================================
        // 1. ARRANGE: Create the graph manually in memory using the new architecture
        // =================================================================
        final String graphName = "TemperatureCheck";

        // --- Create all nodes ---
        ProviderNode currentTemp = new ProviderNode("source.anlageAAS.sensorData.currentTemperature");
        currentTemp.setId(0);
        currentTemp.setName("anlageAAS"); // This name is the key for the dataContext

        ConstantNode tempOffset = new ConstantNode("tempOffset", -2.0);
        tempOffset.setId(1);

        ProviderNode maxThreshold = new ProviderNode("source.anlageAAS.thresholds.maxAllowedTemp");
        maxThreshold.setId(2);
        maxThreshold.setName("anlageAAS"); // Also uses the "anlageAAS" data source

        ConstantNode systemEnabled = new ConstantNode("SystemEnabledFlag", true);
        systemEnabled.setId(3);

        LogicNode addNode = new LogicNode("CorrectedTemp", LogicOperator.ADD);
        addNode.setId(4);

        LogicNode lessThanNode = new LogicNode("TempOK", LogicOperator.LESS_THAN);
        lessThanNode.setId(5);

        LogicNode finalAndNode = new LogicNode("FinalResult", LogicOperator.AND);
        finalAndNode.setId(6);

        // --- Connect the nodes ---
        addNode.setInputNodes(Arrays.asList(currentTemp, tempOffset));
        lessThanNode.setInputNodes(Arrays.asList(addNode, maxThreshold));
        finalAndNode.setInputNodes(Arrays.asList(lessThanNode, systemEnabled));

        List<Node> originalGraph = Arrays.asList(currentTemp, tempOffset, maxThreshold, systemEnabled, addNode, lessThanNode, finalAndNode);

        // =================================================================
        // 2. ACT: Save the graph and load it back
        // =================================================================
        LogicGraphEntity savedEntity = storageService.persistGraph(graphName, originalGraph);

        Optional<List<Node>> loadedGraphOptional = storageService.findGraphById(savedEntity.id);


        // =================================================================
        // 3. ASSERT: Verify the loaded graph's integrity
        // =================================================================
        assertTrue(loadedGraphOptional.isPresent(), "The graph should be loaded successfully.");
        List<Node> loadedGraph = loadedGraphOptional.get();
        assertEquals(originalGraph.size(), loadedGraph.size(), "Loaded graph should have the same number of nodes.");

        // Detailed check on the final node to ensure connections are correct
        Node loadedFinalNode = loadedGraph.stream()
                .filter(n -> n.getId() == 6)
                .findFirst().orElseThrow();

        assertEquals(2, loadedFinalNode.getInputNodes().size(), "The final node should have 2 inputs.");
        assertEquals(5, loadedFinalNode.getInputNodes().get(0).getId(), "First input should be the LESS_THAN node (ID 5).");
        assertEquals(3, loadedFinalNode.getInputNodes().get(1).getId(), "Second input should be the system enabled flag (ID 3).");

        // =================================================================
        // 4. ACT & ASSERT: Evaluate the LOADED graph
        // =================================================================

        // Prepare the runtime data context
        ObjectMapper mapper = new ObjectMapper();
        JsonNode aasData = null;
        try {
            String jsonData = "{\"sensorData\":{\"currentTemperature\":28.0},\"thresholds\":{\"maxAllowedTemp\":30.0}}";
            aasData = mapper.readTree(jsonData);
        } catch (Exception e) {
            fail("Failed to create test JSON data.");
        }

        Map<String, JsonNode> context = new HashMap<>();
        context.put("anlageAAS", aasData); // The key matches the 'name' of the ProviderNodes

        // Expected logic: (28.0 + (-2.0)) < 30.0  &&  true  ==>  26.0 < 30.0  &&  true  ==>  true && true ==> true
        boolean evaluationResult = evaluator.evaluateGraph(loadedGraph, context);

        assertTrue(evaluationResult, "The evaluation of the loaded graph should result in 'true'.");


    }

}