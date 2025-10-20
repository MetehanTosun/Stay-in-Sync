package cache;

import de.unistuttgart.graphengine.cache.GraphInstanceCache;
import de.unistuttgart.graphengine.cache.StatefulLogicGraph;
import de.unistuttgart.graphengine.exception.GraphConstructionException;
import de.unistuttgart.graphengine.nodes.ConfigNode;
import de.unistuttgart.graphengine.nodes.FinalNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GraphInstanceCache Tests")
public class GraphInstanceCacheTest {

    private GraphInstanceCache cache;
    private List<Node> testGraph;

    @BeforeEach
    void setUp() {
        cache = new GraphInstanceCache();
        testGraph = createValidGraph();
    }

    @Test
    @DisplayName("should create new instance on cache miss")
    void testCacheMiss() {
        StatefulLogicGraph graph = cache.getOrCreate(1L, "hash123", testGraph);

        assertNotNull(graph);
        assertEquals(1, cache.getCacheSize());
    }

    @Test
    @DisplayName("should return same instance on cache hit")
    void testCacheHit() {
        StatefulLogicGraph graph1 = cache.getOrCreate(1L, "hash123", testGraph);

        StatefulLogicGraph graph2 = cache.getOrCreate(1L, "hash123", testGraph);

        assertSame(graph1, graph2);
        assertEquals(1, cache.getCacheSize());
    }

    @Test
    @DisplayName("should create separate instances for different hashes")
    void testDifferentHashesCreateSeparateInstances() {
        StatefulLogicGraph graph1 = cache.getOrCreate(1L, "hash123", testGraph);
        StatefulLogicGraph graph2 = cache.getOrCreate(1L, "hash456", testGraph);

        assertNotSame(graph1, graph2);
        assertEquals(2, cache.getCacheSize());
    }

    @Test
    @DisplayName("should create separate instances for different transformation IDs")
    void testDifferentTransformationIdsCreateSeparateInstances() {
        StatefulLogicGraph graph1 = cache.getOrCreate(1L, "hash123", testGraph);
        StatefulLogicGraph graph2 = cache.getOrCreate(2L, "hash123", testGraph);

        assertNotSame(graph1, graph2);
        assertEquals(2, cache.getCacheSize());
    }

    @Test
    @DisplayName("should throw exception when graphHash is null")
    void testNullGraphHash() {
        GraphConstructionException exception = assertThrows(
                GraphConstructionException.class,
                () -> cache.getOrCreate(1L, null, testGraph)
        );

        assertEquals(GraphConstructionException.ErrorType.NULL_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("graphHash cannot be null or blank"));
    }

    @Test
    @DisplayName("should throw exception when graphHash is blank")
    void testBlankGraphHash() {
        GraphConstructionException exception = assertThrows(
                GraphConstructionException.class,
                () -> cache.getOrCreate(1L, "   ", testGraph)
        );

        assertEquals(GraphConstructionException.ErrorType.NULL_INPUT, exception.getErrorType());
    }

    @Test
    @DisplayName("should throw exception when graph definition is null")
    void testNullGraphDefinition() {
        GraphConstructionException exception = assertThrows(
                GraphConstructionException.class,
                () -> cache.getOrCreate(1L, "hash123", null)
        );

        assertEquals(GraphConstructionException.ErrorType.NULL_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("graphDefinition cannot be null"));
    }

    @Test
    @DisplayName("should remove all instances for transformation ID")
    void testRemoveByTransformationId() {
        cache.getOrCreate(1L, "hash123", testGraph);
        cache.getOrCreate(1L, "hash456", testGraph);
        cache.getOrCreate(2L, "hash789", testGraph);

        int removed = cache.remove(1L);

        assertEquals(2, removed);
        assertEquals(1, cache.getCacheSize());
    }

    @Test
    @DisplayName("should return 0 when removing non-existent transformation ID")
    void testRemoveNonExistentId() {
        cache.getOrCreate(1L, "hash123", testGraph);

        int removed = cache.remove(999L);

        assertEquals(0, removed);
        assertEquals(1, cache.getCacheSize());
    }

    @Test
    @DisplayName("should clear all cache entries")
    void testClear() {
        cache.getOrCreate(1L, "hash123", testGraph);
        cache.getOrCreate(2L, "hash456", testGraph);
        cache.getOrCreate(3L, "hash789", testGraph);
        assertEquals(3, cache.getCacheSize());

        cache.clear();

        assertEquals(0, cache.getCacheSize());
    }

    @Test
    @DisplayName("should handle concurrent access to same key")
    void testConcurrentAccess() throws InterruptedException {
        final long transformationId = 1L;
        final String hash = "hash123";
        final StatefulLogicGraph[] results = new StatefulLogicGraph[2];

        Thread thread1 = new Thread(() -> {
            results[0] = cache.getOrCreate(transformationId, hash, testGraph);
        });

        Thread thread2 = new Thread(() -> {
            results[1] = cache.getOrCreate(transformationId, hash, testGraph);
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        assertNotNull(results[0]);
        assertNotNull(results[1]);
        assertSame(results[0], results[1]);
        assertEquals(1, cache.getCacheSize());
    }

    // Helper method
    private List<Node> createValidGraph() {
        ConfigNode configNode = new ConfigNode();
        configNode.setId(1);

        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);
        finalNode.setInputNodes(Arrays.asList(configNode));

        return Arrays.asList(configNode, finalNode);
    }
}