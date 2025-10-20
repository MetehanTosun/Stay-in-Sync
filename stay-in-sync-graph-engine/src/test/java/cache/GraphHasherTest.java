package cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.cache.GraphHasher;
import de.unistuttgart.graphengine.exception.GraphSerializationException;
import de.unistuttgart.graphengine.nodes.ConstantNode;
import de.unistuttgart.graphengine.nodes.FinalNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GraphHasher Tests")
public class GraphHasherTest {

    private GraphHasher graphHasher;

    @BeforeEach
    void setUp() {
        graphHasher = new GraphHasher();
        graphHasher.setObjectMapper(new ObjectMapper());
    }

    @Test
    @DisplayName("should generate consistent hash for same graph")
    void testConsistentHash() {
        List<Node> graph = createSimpleGraph();

        String hash1 = graphHasher.hash(graph);
        String hash2 = graphHasher.hash(graph);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    @DisplayName("should generate different hash for different graphs")
    void testDifferentHashForDifferentGraphs() {
        List<Node> graph1 = createSimpleGraph();

        ConstantNode node = new ConstantNode("DifferentValue", 999);
        node.setId(99);
        List<Node> graph2 = Arrays.asList(node);

        String hash1 = graphHasher.hash(graph1);
        String hash2 = graphHasher.hash(graph2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("should throw exception when graph nodes are null")
    void testNullGraphNodes() {
        GraphSerializationException exception = assertThrows(
                GraphSerializationException.class,
                () -> graphHasher.hash(null)
        );

        assertEquals(GraphSerializationException.ErrorType.INVALID_FORMAT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Cannot hash null graph node list"));
    }

    @Test
    @DisplayName("should handle empty graph list")
    void testEmptyGraphList() {
        List<Node> emptyGraph = new ArrayList<>();

        String hash = graphHasher.hash(emptyGraph);

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("should generate same hash when called multiple times (ThreadLocal reuse)")
    void testThreadLocalReuse() {
        List<Node> graph = createSimpleGraph();

        String hash1 = graphHasher.hash(graph);
        String hash2 = graphHasher.hash(graph);
        String hash3 = graphHasher.hash(graph);

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    @DisplayName("should generate hex string with only valid characters")
    void testHashFormatIsHexadecimal() {
        List<Node> graph = createSimpleGraph();

        String hash = graphHasher.hash(graph);

        assertTrue(hash.matches("[0-9a-f]{64}"), "Hash should be 64 hex characters");
    }

    @Test
    @DisplayName("should handle serialization failure gracefully")
    void testSerializationFailure() {
        GraphHasher hasherWithBrokenMapper = new GraphHasher();
        ObjectMapper brokenMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("Simulated serialization failure") {};
            }
        };
        hasherWithBrokenMapper.setObjectMapper(brokenMapper);
        List<Node> graph = createSimpleGraph();

        GraphSerializationException exception = assertThrows(
                GraphSerializationException.class,
                () -> hasherWithBrokenMapper.hash(graph)
        );

        assertEquals(GraphSerializationException.ErrorType.SERIALIZATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Failed to serialize graph"));
    }

    @Test
    @DisplayName("should successfully create MessageDigest for SHA-256")
    void testCreateMessageDigest() {
        MessageDigest digest = GraphHasher.createMessageDigest();

        assertNotNull(digest);
        assertEquals("SHA-256", digest.getAlgorithm());
    }

    // Helper method
    private List<Node> createSimpleGraph() {
        ConstantNode constantNode = new ConstantNode("TestValue", 42);
        constantNode.setId(1);

        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);
        finalNode.setInputNodes(Arrays.asList(constantNode));

        return Arrays.asList(constantNode, finalNode);
    }
}