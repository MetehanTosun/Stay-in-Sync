package service;

import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.service.GraphTopologicalSorter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphTopologicalSorterTest {

    private GraphTopologicalSorter sorter;

    @BeforeEach
    void setUp() {
        sorter = new GraphTopologicalSorter();
    }

    @Test
    @DisplayName("should return an empty result for an empty graph")
    void shouldReturnEmptyResultForEmptyGraph() {
        // ARRANGE
        List<Node> graphNodes = Collections.emptyList();

        // ACT
        GraphTopologicalSorter.SortResult result = sorter.sort(graphNodes);

        // ASSERT
        assertTrue(result.sortedNodes().isEmpty());
        assertFalse(result.hasCycle());
        assertTrue(result.cycleNodeIds().isEmpty());
    }

    @Test
    @DisplayName("should sort a graph with a single node")
    void shouldSortSingleNodeGraph() {
        // ARRANGE
        Node nodeA = createMockNode(1, Collections.emptyList());
        List<Node> graphNodes = List.of(nodeA);

        // ACT
        GraphTopologicalSorter.SortResult result = sorter.sort(graphNodes);

        // ASSERT
        assertEquals(1, result.sortedNodes().size());
        assertEquals(nodeA, result.sortedNodes().get(0));
        assertFalse(result.hasCycle());
    }

    @Test
    @DisplayName("should sort a simple linear graph correctly (A -> B -> C)")
    void shouldSortSimpleLinearGraph() {
        // ARRANGE
        Node nodeA = createMockNode(1, Collections.emptyList());
        Node nodeB = createMockNode(2, List.of(nodeA));
        Node nodeC = createMockNode(3, List.of(nodeB));
        List<Node> graphNodes = List.of(nodeA, nodeB, nodeC);

        // ACT
        GraphTopologicalSorter.SortResult result = sorter.sort(graphNodes);

        // ASSERT
        assertFalse(result.hasCycle());
        assertEquals(3, result.sortedNodes().size());
        // Verify the topological order
        List<Integer> sortedIds = getNodeIds(result.sortedNodes());
        assertEquals(List.of(1, 2, 3), sortedIds);
    }

    @Test
    @DisplayName("should sort a graph with multiple dependencies (A -> C, B -> C)")
    void shouldSortGraphWithMultipleDependencies() {
        // ARRANGE
        Node nodeA = createMockNode(1, Collections.emptyList());
        Node nodeB = createMockNode(2, Collections.emptyList());
        Node nodeC = createMockNode(3, List.of(nodeA, nodeB));
        List<Node> graphNodes = List.of(nodeA, nodeB, nodeC);

        // ACT
        GraphTopologicalSorter.SortResult result = sorter.sort(graphNodes);

        // ASSERT
        assertFalse(result.hasCycle());
        assertEquals(3, result.sortedNodes().size());
        // A and B must appear before C
        List<Integer> sortedIds = getNodeIds(result.sortedNodes());
        assertTrue(sortedIds.indexOf(1) < sortedIds.indexOf(3));
        assertTrue(sortedIds.indexOf(2) < sortedIds.indexOf(3));
    }

    @Test
    @DisplayName("should detect a simple, direct cycle (A -> B -> A)")
    void shouldDetectSimpleDirectCycle() {
        // ARRANGE
        Node nodeA = mock(Node.class);
        Node nodeB = mock(Node.class);

        // Create the cycle A -> B -> A
        when(nodeA.getId()).thenReturn(1);
        when(nodeA.getInputNodes()).thenReturn(List.of(nodeB));
        when(nodeA.toString()).thenReturn("Node(1)");

        when(nodeB.getId()).thenReturn(2);
        when(nodeB.getInputNodes()).thenReturn(List.of(nodeA));
        when(nodeB.toString()).thenReturn("Node(2)");

        List<Node> graphNodes = List.of(nodeA, nodeB);

        // ACT
        GraphTopologicalSorter.SortResult result = sorter.sort(graphNodes);

        // ASSERT
        assertTrue(result.hasCycle());
        // Nodes in a cycle will not be in the sorted list
        assertTrue(result.sortedNodes().isEmpty());
        // The IDs of the cycle nodes should be reported
        assertEquals(2, result.cycleNodeIds().size());
        assertTrue(result.cycleNodeIds().containsAll(List.of(1, 2)));
    }

    @Test
    @DisplayName("should detect a larger cycle with a starting node (A -> B -> C -> B)")
    void shouldDetectLargerCycle() {
        // ARRANGE
        Node nodeA = createMockNode(1, Collections.emptyList());
        Node nodeB = mock(Node.class);
        Node nodeC = mock(Node.class);

        // Create the cycle B -> C -> B
        when(nodeB.getId()).thenReturn(2);
        when(nodeB.getInputNodes()).thenReturn(List.of(nodeA, nodeC)); // Depends on A and C
        when(nodeB.toString()).thenReturn("Node(2)");

        when(nodeC.getId()).thenReturn(3);
        when(nodeC.getInputNodes()).thenReturn(List.of(nodeB)); // Depends on B
        when(nodeC.toString()).thenReturn("Node(3)");

        List<Node> graphNodes = List.of(nodeA, nodeB, nodeC);

        // ACT
        GraphTopologicalSorter.SortResult result = sorter.sort(graphNodes);

        // ASSERT
        assertTrue(result.hasCycle());
        // Only node A, which is not part of the cycle, can be sorted
        assertEquals(1, result.sortedNodes().size());
        assertEquals(nodeA, result.sortedNodes().get(0));
        // Nodes B and C form the cycle
        assertEquals(2, result.cycleNodeIds().size());
        assertTrue(result.cycleNodeIds().containsAll(List.of(2, 3)));
    }

    @Test
    @DisplayName("should correctly handle nodes with null input list")
    void shouldHandleNodesWithNullInputList() {
        // ARRANGE
        Node nodeA = createMockNode(1, null); // Has null inputs, should be treated as in-degree 0
        Node nodeB = createMockNode(2, List.of(nodeA));
        List<Node> graphNodes = List.of(nodeA, nodeB);

        // ACT
        GraphTopologicalSorter.SortResult result = sorter.sort(graphNodes);

        // ASSERT
        assertFalse(result.hasCycle());
        assertEquals(2, result.sortedNodes().size());
        assertEquals(List.of(1, 2), getNodeIds(result.sortedNodes()));
    }

    // ==========================================================================================
    // Helper Methods
    // ==========================================================================================

    /**
     * Creates a mock Node for testing purposes.
     * @param id The ID of the node.
     * @param parents The list of parent nodes (inputs).
     * @return A configured mock Node.
     */
    private Node createMockNode(int id, List<Node> parents) {
        Node node = mock(Node.class);
        when(node.getId()).thenReturn(id);
        when(node.getInputNodes()).thenReturn(parents);
        // Add a toString() mock for easier debugging and clearer test failure messages
        when(node.toString()).thenReturn("Node(" + id + ")");
        return node;
    }

    /**
     * Extracts the IDs from a list of nodes.
     * @param nodes The list of nodes.
     * @return A list of their IDs.
     */
    private List<Integer> getNodeIds(List<Node> nodes) {
        return nodes.stream()
                .map(Node::getId)
                .collect(Collectors.toList());
    }
}