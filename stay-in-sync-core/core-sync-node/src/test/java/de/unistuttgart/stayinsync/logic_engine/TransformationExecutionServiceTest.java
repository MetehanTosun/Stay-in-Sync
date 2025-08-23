package de.unistuttgart.stayinsync.logic_engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.stayinsync.syncnode.logic_engine.SnapshotCacheService;
import de.unistuttgart.stayinsync.syncnode.syncjob.TransformationExecutionService;
import de.unistuttgart.stayinsync.transport.exception.NodeConfigurationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.*;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphTopologicalSorter;
import io.smallrye.mutiny.Uni;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration-style tests for the TransformationExecutionService.
 * This test uses a real LogicGraphEvaluator and mocks external dependencies.
 */
@ExtendWith(MockitoExtension.class)
public class TransformationExecutionServiceTest {

    // --- System Under Test ---
    private TransformationExecutionService executionService;

    // --- Mocked Dependencies ---
    @Mock
    private SnapshotCacheService snapshotCache;
    @Mock
    private ScriptEngineService scriptEngineService;
    @Mock
    private ManagedExecutor managedExecutor;

    // --- Real Components ---
    private LogicGraphEvaluator realEvaluator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        // 1. Erstelle die ECHTEN Komponenten
        realEvaluator = new LogicGraphEvaluator();
        realEvaluator.setSorter(new GraphTopologicalSorter());

        // 2. Erstelle die zu testende Service-Instanz MANUELL
        executionService = new TransformationExecutionService();

        // 3. Injiziere alle Abhängigkeiten mit Reflection
        setField(executionService, "snapshotCache", this.snapshotCache);
        setField(executionService, "logicGraphEvaluator", this.realEvaluator);
        setField(executionService, "scriptEngineService", this.scriptEngineService);
        setField(executionService, "managedExecutor", this.managedExecutor);
        setField(executionService, "objectMapper", this.objectMapper);

        // Konfiguriere den Executor-Mock
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(managedExecutor).execute(any(Runnable.class));
    }

    // Helper-Methode für Reflection
    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }

    @Test
    void testFullLifecycle_WhenChangeIsDetected_ShouldFetchSaveAndExecute() throws IOException {
        // 1. ARRANGE (Vorbereiten)
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConfigNode configNode = createConfigNode(1, tempProvider);
        FinalNode finalNode = createFinalNode(2, configNode);
        List<Node> graph = Arrays.asList(tempProvider, configNode, finalNode);

        long transformationId = 123L;
        Map<String, Object> sourceData = Map.of("source", Map.of("sensor", Map.of("temperature", 25.0)));
        TransformJob job = new TransformJob("Transformation-" + transformationId, String.valueOf(transformationId), "script-1", "", "js", "", sourceData);

        JsonNode oldSnapshot = objectMapper.valueToTree(Map.of("source.sensor.temperature", new SnapshotEntry(20.0, 0L)));

        // Konfiguriere die Mocks
        when(snapshotCache.getSnapshot(transformationId)).thenReturn(Optional.of(oldSnapshot));
        when(scriptEngineService.transformAsync(any(TransformJob.class))).thenReturn(Uni.createFrom().nullItem());

        // 2. ACT (Ausführen)
        executionService.execute(job, graph).await().indefinitely();

        // 3. ASSERT (Überprüfen)

        // a) Wurde der alte Snapshot korrekt vom Cache geholt?
        verify(snapshotCache, times(1)).getSnapshot(transformationId);

        // b) Wurde der neue Snapshot korrekt im Cache gespeichert?
        ArgumentCaptor<JsonNode> snapshotCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(snapshotCache, times(1)).saveSnapshot(eq(transformationId), snapshotCaptor.capture());
        assertEquals(25.0, snapshotCaptor.getValue().get("source.sensor.temperature").get("value").asDouble());

        // c) Wurde das Skript ausgeführt, weil eine Änderung erkannt wurde?
        verify(scriptEngineService, times(1)).transformAsync(job);
    }

    // HINZUGEFÜGT: Testfall für den "Keine Änderung"-Pfad
    @Test
    void testFullLifecycle_WhenNoChangeIsDetected_ShouldFetchSaveAndNotExecute() throws IOException {
        // 1. ARRANGE
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConfigNode configNode = createConfigNode(1, tempProvider);
        FinalNode finalNode = createFinalNode(2, configNode);
        List<Node> graph = Arrays.asList(tempProvider, configNode, finalNode);

        long transformationId = 123L;
        // Der Live-Wert (20.0) ist identisch mit dem Snapshot-Wert
        Map<String, Object> sourceData = Map.of("source", Map.of("sensor", Map.of("temperature", 20.0)));
        TransformJob job = new TransformJob("Transformation-" + transformationId, String.valueOf(transformationId), "script-1", "", "js", "", sourceData);

        JsonNode oldSnapshot = objectMapper.valueToTree(Map.of("source.sensor.temperature", new SnapshotEntry(20.0, 0L)));

        when(snapshotCache.getSnapshot(transformationId)).thenReturn(Optional.of(oldSnapshot));

        // 2. ACT
        executionService.execute(job, graph).await().indefinitely();

        // 3. ASSERT

        // a) Der Snapshot wurde trotzdem gespeichert (um den Zeitstempel zu aktualisieren)
        verify(snapshotCache, times(1)).saveSnapshot(eq(transformationId), any(JsonNode.class));

        // b) Das Skript wurde NICHT ausgeführt
        verify(scriptEngineService, never()).transformAsync(any(TransformJob.class));
    }

    // =====================================================================================
    // HELFER-METHODEN
    // =====================================================================================
    private ProviderNode createProviderNode(String jsonPath, int id) {  try { ProviderNode node = new ProviderNode(jsonPath); node.setId(id); return node; } catch (NodeConfigurationException e) { throw new RuntimeException(e); } }
    private ConfigNode createConfigNode(int id, Node... inputs) { ConfigNode node = new ConfigNode(); node.setId(id); node.setInputNodes(Arrays.asList(inputs)); return node; }
    private FinalNode createFinalNode(int id, Node input) { FinalNode node = new FinalNode(); node.setId(id); node.setInputNodes(List.of(input)); return node; }
}