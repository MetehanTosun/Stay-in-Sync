package validation_error;

import de.unistuttgart.graphengine.validation_error.CycleError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("CycleError Tests")
public class CycleErrorTest {

    @Test
    @DisplayName("should format message with single node in cycle")
    void testGetMessage_WithSingleNode_ShouldFormatCorrectly() {
        // ARRANGE
        List<Integer> nodeIds = Arrays.asList(1);
        CycleError error = new CycleError(nodeIds);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: [1]", result);
    }

    @Test
    @DisplayName("should format message with multiple nodes in cycle")
    void testGetMessage_WithMultipleNodes_ShouldFormatCorrectly() {
        // ARRANGE
        List<Integer> nodeIds = Arrays.asList(1, 2, 3);
        CycleError error = new CycleError(nodeIds);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: [1, 2, 3]", result);
        assertTrue(result.contains("1, 2, 3"));
    }

    @Test
    @DisplayName("should format message with empty list")
    void testGetMessage_WithEmptyList_ShouldHandleGracefully() {
        // ARRANGE
        List<Integer> nodeIds = Arrays.asList();
        CycleError error = new CycleError(nodeIds);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: []", result);
    }

    @Test
    @DisplayName("should format message with large numbers")
    void testGetMessage_WithLargeNumbers_ShouldFormatCorrectly() {
        // ARRANGE
        List<Integer> nodeIds = Arrays.asList(999, 1000, 2024);
        CycleError error = new CycleError(nodeIds);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: [999, 1000, 2024]", result);
    }

    @Test
    @DisplayName("should return correct error code")
    void testGetErrorCode_ShouldReturnCycleDetected() {
        // ARRANGE
        CycleError error = new CycleError(Arrays.asList(1, 2));

        // ACT & ASSERT
        assertEquals("CYCLE_DETECTED", error.getErrorCode());
    }

    @Test
    @DisplayName("should handle null list gracefully")
    void testGetMessage_WithNullList_ShouldHandleGracefully() {
        // ARRANGE
        CycleError error = new CycleError(null);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: null", result);
    }

    @Test
    @DisplayName("should handle very large node lists in cycle")
    void testGetMessage_WithLargeNodeList_ShouldFormatCorrectly() {
        // ARRANGE
        List<Integer> largeNodeList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        CycleError error = new CycleError(largeNodeList);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]", result);
        assertTrue(result.contains("1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12"));
    }

    @Test
    @DisplayName("should handle negative node IDs in cycle")
    void testGetMessage_WithNegativeNodeIds_ShouldFormatCorrectly() {
        // ARRANGE
        List<Integer> nodeIds = Arrays.asList(-1, -5, -10);
        CycleError error = new CycleError(nodeIds);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: [-1, -5, -10]", result);
        assertTrue(result.contains("-1, -5, -10"));
    }

    @Test
    @DisplayName("should handle zero node IDs in cycle")
    void testGetMessage_WithZeroNodeIds_ShouldFormatCorrectly() {
        // ARRANGE
        List<Integer> nodeIds = Arrays.asList(0, 1, 0);
        CycleError error = new CycleError(nodeIds);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: [0, 1, 0]", result);
        assertTrue(result.contains("0, 1, 0"));
    }

    @Test
    @DisplayName("should handle duplicate node IDs in cycle")
    void testGetMessage_WithDuplicateNodeIds_ShouldFormatCorrectly() {
        // ARRANGE
        List<Integer> nodeIds = Arrays.asList(1, 2, 1, 3, 2);
        CycleError error = new CycleError(nodeIds);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("A cycle was detected involving nodes: [1, 2, 1, 3, 2]", result);
        assertTrue(result.contains("1, 2, 1, 3, 2"));
    }

    @Test
    @DisplayName("should create CycleError with no-args constructor")
    void testNoArgsConstructor_ShouldCreateInstance() {
        // ACT
        CycleError error = new CycleError();

        // ASSERT
        assertNotNull(error);
        assertEquals("CYCLE_DETECTED", error.getErrorCode());
    }

    @Test
    @DisplayName("should handle getter and setter for nodeIdsInCycle")
    void testGetterSetter_ForNodeIdsInCycle() {
        // ARRANGE
        CycleError error = new CycleError();
        List<Integer> nodeIds = Arrays.asList(10, 20, 30);

        // ACT
        error.setNodeIdsInCycle(nodeIds);

        // ASSERT
        assertEquals(nodeIds, error.getNodeIdsInCycle());
    }

    @Test
    @DisplayName("should be consistent across multiple getMessage calls")
    void testGetMessage_ConsistentAcrossMultipleCalls() {
        // ARRANGE
        List<Integer> nodeIds = Arrays.asList(5, 10, 15);
        CycleError error = new CycleError(nodeIds);

        // ACT
        String result1 = error.getMessage();
        String result2 = error.getMessage();
        String result3 = error.getMessage();

        // ASSERT
        assertEquals(result1, result2);
        assertEquals(result2, result3);
        assertEquals("A cycle was detected involving nodes: [5, 10, 15]", result1);
    }}
