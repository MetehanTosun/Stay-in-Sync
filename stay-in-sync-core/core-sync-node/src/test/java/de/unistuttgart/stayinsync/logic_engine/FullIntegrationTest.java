package de.unistuttgart.stayinsync.logic_engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.stayinsync.syncnode.logic_engine.SnapshotCacheService;
import de.unistuttgart.stayinsync.syncnode.syncjob.TransformationExecutionService;
import de.unistuttgart.stayinsync.transport.exception.NodeConfigurationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.*;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphTopologicalSorter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * End-to-End test for the change detection lifecycle.
 * This test uses REAL instances of the execution service, evaluator, and cache service.
 */
@ExtendWith(MockitoExtension.class)
public class FullIntegrationTest {

    // --- System Under Test (SUT) ---
    private TransformationExecutionService executionService;

    // --- Mocked Dependencies ---
    @Mock
    private ScriptEngineService scriptEngineService;
    @Mock
    private ManagedExecutor managedExecutor;

    // --- REAL Components that work together ---
    private SnapshotCacheService realCache;
    private LogicGraphEvaluator realEvaluator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 1. Create all REAL service instances
        realCache = new SnapshotCacheService();
        realEvaluator = new LogicGraphEvaluator();
        realEvaluator.setSorter(new GraphTopologicalSorter());

        // 2. Create the SUT and manually inject its dependencies
        executionService = new TransformationExecutionService();
        setField(executionService, "snapshotCache", this.realCache);
        setField(executionService, "logicGraphEvaluator", this.realEvaluator);
        setField(executionService, "scriptEngineService", this.scriptEngineService);
        setField(executionService, "objectMapper", this.objectMapper);
        setField(executionService, "managedExecutor", this.managedExecutor);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(managedExecutor).execute(any(Runnable.class));
    }

    @Test
    void testEndToEnd_WithChangeAndNoChangeAndChangeAgain() throws IOException {
        System.out.println("=== TEST STARTED ===");
        // ARRANGE
        long transformationId = 123L;
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConfigNode configNode = createConfigNode(1, tempProvider);
        FinalNode finalNode = createFinalNode(2, configNode);
        List<Node> graph = Arrays.asList(tempProvider, configNode, finalNode);
        System.out.println("=== GRAPH CREATED ===");

        // Configure the script engine mock to do nothing
        when(scriptEngineService.transformAsync(any(TransformJob.class))).thenReturn(Uni.createFrom().nullItem());

        // === RUN 1: A change is detected (no initial snapshot) ===

        // ACT 1
        System.out.println("=== STARTING RUN 1 ===");
        Map<String, Object> sourceData1 = Map.of("source", Map.of("sensor", Map.of("temperature", 25.0)));
        TransformJob job1 = new TransformJob("Transformation-" + transformationId, String.valueOf(transformationId), "script-1", "", "js", "", sourceData1);
        System.out.println("=== CALLING EXECUTE ===");
        executionService.execute(job1, graph).await().indefinitely();
        System.out.println("=== RUN 1 COMPLETED ===");

        // ASSERT 1
        verify(scriptEngineService, times(1)).transformAsync(any(TransformJob.class));
        Optional<JsonNode> savedSnapshot1 = realCache.getSnapshot(transformationId);
        assertTrue(savedSnapshot1.isPresent(), "A snapshot should have been saved after the first run.");
        assertEquals(25.0, savedSnapshot1.get().get("source.sensor.temperature").get("value").asDouble());


        // === RUN 2: No change is detected ===

        // ACT 2
        Map<String, Object> sourceData2 = Map.of("source", Map.of("sensor", Map.of("temperature", 25.0)));
        TransformJob job2 = new TransformJob("Transformation-" + transformationId, String.valueOf(transformationId), "script-1", "", "js", "", sourceData2);
        executionService.execute(job2, graph).await().indefinitely();

        // ASSERT 2
        // The script engine should NOT have been called again (total calls is still 1).
        verify(scriptEngineService, times(1)).transformAsync(any(TransformJob.class));


        // === RUN 3: A new change is detected ===

        // ACT 3
        Map<String, Object> sourceData3 = Map.of("source", Map.of("sensor", Map.of("temperature", 30.0)));
        TransformJob job3 = new TransformJob("Transformation-" + transformationId, String.valueOf(transformationId), "script-1", "", "js", "", sourceData3);
        executionService.execute(job3, graph).await().indefinitely();

        // ASSERT 3
        // The script engine SHOULD have been called again (total calls is now 2).
        verify(scriptEngineService, times(2)).transformAsync(any(TransformJob.class));
        // The cache should now contain the newest state.
        Optional<JsonNode> savedSnapshot3 = realCache.getSnapshot(transformationId);
        assertTrue(savedSnapshot3.isPresent(), "Snapshot should still exist after the third run.");
        assertEquals(30.0, savedSnapshot3.get().get("source.sensor.temperature").get("value").asDouble());
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // HELPER METHODS
    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    private ProviderNode createProviderNode(String jsonPath, int id) {  try { ProviderNode node = new ProviderNode(jsonPath); node.setId(id); return node; } catch (NodeConfigurationException e) { throw new RuntimeException(e); } }
    private ConfigNode createConfigNode(int id, Node... inputs) { ConfigNode node = new ConfigNode(); node.setId(id); node.setInputNodes(Arrays.asList(inputs)); return node; }
    private FinalNode createFinalNode(int id, Node input) { FinalNode node = new FinalNode(); node.setId(id); node.setInputNodes(List.of(input)); return node; }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}
