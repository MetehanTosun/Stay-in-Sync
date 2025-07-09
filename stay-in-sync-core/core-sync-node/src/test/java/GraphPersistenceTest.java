package de.unistuttgart.stayinsync.syncnode.logik_engine;


import de.unistuttgart.stayinsync.syncnode.logik_engine.database.GraphStorageService;
import de.unistuttgart.stayinsync.syncnode.logik_engine.database.LogicGraphEntity;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ProviderNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.ConstantNode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the GraphStorageService.
 * This test covers the full lifecycle: saving a graph to the database,
 * loading it back, and verifying its integrity and functionality.
 */
@QuarkusTest
public class GraphPersistenceTest {

    // Quarkus injects the service, providing access to the database via our logic.
    @Inject
    GraphStorageService storageService;

    // The evaluator is stateless, so we can create it directly.
    LogicGraphEvaluator evaluator = new LogicGraphEvaluator();

    /**
     * This method runs before each test to ensure a clean database state.
     * It prevents tests from interfering with each other.
     */
    @BeforeEach
    @Transactional
    void cleanupDatabase() {
        LogicGraphEntity.deleteAll();
    }

    @Test
    @Transactional
    public void testSaveLoadAndEvaluateGraph() {
        // =================================================================
        // 1. ARRANGE: Create a complex graph manually in memory
        // =================================================================
        final String graphName = "FullIntegrationTestGraph";

        // Define all necessary input nodes
        ProviderNode aasTemp = new ProviderNode("anlageAAS", "sensorData.currentTemperature");
        ConstantNode tempOffset = new ConstantNode("TempOffset", -2.0);
        ProviderNode aasMaxTemp = new ProviderNode("anlageAAS", "thresholds.maxAllowedTemp");
        ConstantNode systemEnabled = new ConstantNode("SystemEnabledFlag", true);

        // Build the graph structure by connecting the nodes
        LogicNode correctedTempNode = new LogicNode("CorrectedTemp", LogicOperator.ADD, aasTemp, tempOffset);
        LogicNode tempOkNode = new LogicNode("TempOK", LogicOperator.LESS_THAN, new ParentNode(correctedTempNode), aasMaxTemp);
        LogicNode finalResultNode = new LogicNode("FinalResult", LogicOperator.AND, new ParentNode(tempOkNode), systemEnabled);

        List<LogicNode> originalGraph = new ArrayList<>();
        originalGraph.add(correctedTempNode);
        originalGraph.add(tempOkNode);
        originalGraph.add(finalResultNode);

        // =================================================================
        // 2. ACT: Save the graph to the database and load it back
        // =================================================================

        // Save the graph
        assertDoesNotThrow(() -> storageService.saveGraph(graphName, originalGraph), "Saving the graph should not throw an exception.");

        // Load the graph back from the database
        Optional<List<LogicNode>> loadedGraphOptional = storageService.loadGraph(graphName);

        // =================================================================
        // 3. ASSERT: Verify the integrity of the loaded graph
        // =================================================================

        // 3a: Check if loading was successful
        assertTrue(loadedGraphOptional.isPresent(), "The graph should be found in the database and loaded successfully.");
        List<LogicNode> loadedGraph = loadedGraphOptional.get();

        // 3b: Check the basic structure
        assertNotNull(loadedGraph, "The loaded graph list should not be null.");
        assertEquals(originalGraph.size(), loadedGraph.size(), "The loaded graph should have the same number of nodes as the original.");

        // 3c: Perform a detailed check on a specific node to ensure connections are correct
        LogicNode loadedFinalNode = loadedGraph.stream()
                .filter(n -> n.getNodeName().equals("FinalResult"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Target node 'FinalResult' not found in loaded graph."));

        assertEquals(2, loadedFinalNode.getInputProviders().size(), "The target node should have 2 inputs.");

        InputNode firstInput = loadedFinalNode.getInputProviders().get(0);
        assertTrue(firstInput.isParentNode(), "The first input of the target node should be a ParentNode.");
        assertEquals("TempOK", firstInput.getParentNode().getNodeName(), "The ParentNode should point to 'TempOK'.");

        // =================================================================
        // 4. ACT & ASSERT: Evaluate the LOADED graph to confirm it's functional
        // =================================================================

        // Prepare the runtime data context
        JsonObject aasData = Json.createObjectBuilder()
                .add("sensorData", Json.createObjectBuilder().add("currentTemperature", 25.0))
                .add("thresholds", Json.createObjectBuilder().add("maxAllowedTemp", 30.0))
                .build();

        Map<String, JsonObject> context = new HashMap<>();
        context.put("anlageAAS", aasData);

        // Evaluate the graph that came FROM THE DATABASE
        // Expected logic: (25.0 + (-2.0)) < 30.0  &&  true  ==>  23.0 < 30.0  &&  true  ==>  true && true ==> true
        boolean evaluationResult = evaluator.evaluateGraph(loadedGraph, context);

        // Assert the final result
        assertTrue(evaluationResult, "The evaluation of the loaded graph should result in 'true'.");

        System.out.println("SUCCESS: Graph was saved, loaded, and evaluated correctly!");

        // =================================================================
        // !!! NUR ZUM DEBUGGEN HINZUFÜGEN !!!
        // =================================================================
        System.out.println("TEST ANGEHALTEN. Drücke Enter im Konsolenfenster, um fortzufahren und den Rollback auszuführen...");
        try {
            System.in.read(); // Der Test wartet hier auf eine Eingabe von dir
        } catch (Exception e) {
            // Ignorieren
        }
    }
}
