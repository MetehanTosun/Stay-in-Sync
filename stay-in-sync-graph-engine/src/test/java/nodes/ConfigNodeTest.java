package nodes;

import de.unistuttgart.graphengine.nodes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@DisplayName("ConfigNode Tests")
public class ConfigNodeTest {

    private ConfigNode configNode;
    private ProviderNode providerNode1;
    private ProviderNode providerNode2;
    private Map<String, Object> dataContext;

    @BeforeEach
    void setUp() {
        configNode = new ConfigNode();
        providerNode1 = new ProviderNode("source.system1.value1");
        providerNode2 = new ProviderNode("source.system2.value2");

        // Setup test nodes with calculated results
        providerNode1.setCalculatedResult("test_value_1");
        providerNode2.setCalculatedResult("test_value_2");

        configNode.setInputNodes(Arrays.asList(providerNode1, providerNode2));

        dataContext = new HashMap<>();
    }

    // ===== BASIC FUNCTIONALITY TESTS =====

    @Test
    @DisplayName("should initialize with default values")
    void testDefaultInitialization() {
        ConfigNode node = new ConfigNode();
        assertEquals(ConfigNode.ChangeDetectionMode.OR, node.getMode());
        assertTrue(node.isActive());
        assertFalse(node.isTimeWindowEnabled());
        assertEquals(30000, node.getTimeWindowMillis());
        assertNull(node.getNewSnapshotData());
    }

    @Test
    @DisplayName("should return Boolean as output type")
    void testGetOutputType() {
        assertEquals(Boolean.class, configNode.getOutputType());
    }

    @Test
    @DisplayName("should set and get test time")
    void testTestTimeHandling() {
        long testTime = 1000000L;
        configNode.setTestTime(testTime);
        assertEquals(testTime, configNode.getTestTime());
    }

    // ===== INACTIVE NODE TESTS =====

    @Test
    @DisplayName("should return false when node is inactive")
    void testCalculate_WhenInactive_ShouldReturnFalse() {
        // ARRANGE
        configNode.setActive(false);

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertFalse((Boolean) configNode.getCalculatedResult());
        assertNotNull(configNode.getNewSnapshotData());
        assertEquals(2, configNode.getNewSnapshotData().size());
    }

    @Test
    @DisplayName("should create snapshot even when inactive")
    void testCalculate_WhenInactive_ShouldCreateSnapshot() {
        // ARRANGE
        configNode.setActive(false);
        configNode.setTestTime(12345L);

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        Map<String, SnapshotEntry> snapshot = configNode.getNewSnapshotData();
        assertTrue(snapshot.containsKey("source.system1.value1"));
        assertTrue(snapshot.containsKey("source.system2.value2"));
        assertEquals(12345L, snapshot.get("source.system1.value1").timestamp());
    }

    // ===== OR MODE TESTS =====

    @Test
    @DisplayName("should initialize snapshot in OR mode with no previous snapshot")
    void testCalculate_OrModeNoPreviousSnapshot_ShouldReturnTrue() {
        // ARRANGE
        configNode.setMode(ConfigNode.ChangeDetectionMode.OR);

        // ACT
        configNode.calculate(dataContext);

        // ASSERT - First execution should return FALSE (only initialize snapshot)
        assertFalse((Boolean) configNode.getCalculatedResult());
        // But snapshot should be created
        assertNotNull(configNode.getNewSnapshotData());
        assertEquals(2, configNode.getNewSnapshotData().size());
    }

    @Test
    @DisplayName("should detect change in OR mode when one value changes")
    void testCalculate_OrModeOneValueChanged_ShouldReturnTrue() {
        // ARRANGE
        configNode.setMode(ConfigNode.ChangeDetectionMode.OR);

        // Create old snapshot with different value - CORRECTED: Direct Map instead of JsonNode
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        oldSnapshot.put("source.system1.value1", new SnapshotEntry("old_value", 1000L));
        oldSnapshot.put("source.system2.value2", new SnapshotEntry("test_value_2", 1000L));

        dataContext.put("__snapshot", oldSnapshot); // CORRECTED: Put Map directly

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertTrue((Boolean) configNode.getCalculatedResult());
    }

    @Test
    @DisplayName("should not detect change in OR mode when no values change")
    void testCalculate_OrModeNoValuesChanged_ShouldReturnFalse() {
        // ARRANGE
        configNode.setMode(ConfigNode.ChangeDetectionMode.OR);

        // Create old snapshot with same values - CORRECTED: Direct Map instead of JsonNode
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        oldSnapshot.put("source.system1.value1", new SnapshotEntry("test_value_1", 1000L));
        oldSnapshot.put("source.system2.value2", new SnapshotEntry("test_value_2", 1000L));

        dataContext.put("__snapshot", oldSnapshot); // CORRECTED: Put Map directly

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertFalse((Boolean) configNode.getCalculatedResult());
    }

    // ===== AND MODE TESTS =====

    @Test
    @DisplayName("should detect change in AND mode when all values change")
    void testCalculate_AndModeAllValuesChanged_ShouldReturnTrue() {
        // ARRANGE
        configNode.setMode(ConfigNode.ChangeDetectionMode.AND);

        // Create old snapshot with different values - CORRECTED: Direct Map instead of JsonNode
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        oldSnapshot.put("source.system1.value1", new SnapshotEntry("old_value_1", 1000L));
        oldSnapshot.put("source.system2.value2", new SnapshotEntry("old_value_2", 1000L));

        dataContext.put("__snapshot", oldSnapshot); // CORRECTED: Put Map directly

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertTrue((Boolean) configNode.getCalculatedResult());
    }

    @Test
    @DisplayName("should not detect change in AND mode when only some values change")
    void testCalculate_AndModePartialChange_ShouldReturnFalse() {
        // ARRANGE
        configNode.setMode(ConfigNode.ChangeDetectionMode.AND);

        // Create old snapshot with one same value - CORRECTED: Direct Map instead of JsonNode
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        oldSnapshot.put("source.system1.value1", new SnapshotEntry("old_value_1", 1000L));
        oldSnapshot.put("source.system2.value2", new SnapshotEntry("test_value_2", 1000L)); // Same value

        dataContext.put("__snapshot", oldSnapshot); // CORRECTED: Put Map directly

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertFalse((Boolean) configNode.getCalculatedResult());
    }

    // ===== TIME WINDOW TESTS =====

    @Test
    @DisplayName("should handle time window in OR mode")
    void testCalculate_TimeWindowOrMode_ShouldWork() {
        // ARRANGE
        configNode.setMode(ConfigNode.ChangeDetectionMode.OR);
        configNode.setTimeWindowEnabled(true);
        configNode.setTimeWindowMillis(10000L); // 10 seconds
        configNode.setTestTime(15000L); // Current time

        // Create old snapshot with timestamps within window - CORRECTED: Direct Map instead of JsonNode
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        oldSnapshot.put("source.system1.value1", new SnapshotEntry("test_value_1", 10000L)); // Within window
        oldSnapshot.put("source.system2.value2", new SnapshotEntry("test_value_2", 2000L));  // Outside window

        dataContext.put("__snapshot", oldSnapshot); // CORRECTED: Put Map directly

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertTrue((Boolean) configNode.getCalculatedResult()); // At least one value in window
    }

    @Test
    @DisplayName("should handle time window in AND mode")
    void testCalculate_TimeWindowAndMode_ShouldWork() {
        // ARRANGE
        configNode.setMode(ConfigNode.ChangeDetectionMode.AND);
        configNode.setTimeWindowEnabled(true);
        configNode.setTimeWindowMillis(10000L);
        configNode.setTestTime(15000L);

        // Create old snapshot with one timestamp outside window - CORRECTED: Direct Map instead of JsonNode
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        oldSnapshot.put("source.system1.value1", new SnapshotEntry("test_value_1", 10000L)); // Within window
        oldSnapshot.put("source.system2.value2", new SnapshotEntry("test_value_2", 2000L));  // Outside window

        dataContext.put("__snapshot", oldSnapshot); // CORRECTED: Put Map directly

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertFalse((Boolean) configNode.getCalculatedResult()); // Not all values in window
    }

    @Test
    @DisplayName("should return true in time window AND mode when all values are in window")
    void testCalculate_TimeWindowAndModeAllInWindow_ShouldReturnTrue() {
        // ARRANGE
        configNode.setMode(ConfigNode.ChangeDetectionMode.AND);
        configNode.setTimeWindowEnabled(true);
        configNode.setTimeWindowMillis(10000L);
        configNode.setTestTime(15000L);

        // Create old snapshot with all timestamps within window - CORRECTED: Direct Map instead of JsonNode
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        oldSnapshot.put("source.system1.value1", new SnapshotEntry("test_value_1", 10000L)); // Within window
        oldSnapshot.put("source.system2.value2", new SnapshotEntry("test_value_2", 12000L)); // Within window

        dataContext.put("__snapshot", oldSnapshot); // CORRECTED: Put Map directly

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertTrue((Boolean) configNode.getCalculatedResult());
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("should handle null snapshot in data context")
    void testCalculate_WithNullSnapshot_ShouldWork() {
        // ARRANGE
        dataContext.put("__snapshot", null);

        // ACT
        configNode.calculate(dataContext);

        // ASSERT - First execution (null snapshot) should return FALSE
        assertFalse((Boolean) configNode.getCalculatedResult());
    }

    @Test
    @DisplayName("should handle missing snapshot in data context")
    void testCalculate_WithMissingSnapshot_ShouldWork() {
        // ARRANGE
        // dataContext has no "__snapshot" key

        // ACT
        configNode.calculate(dataContext);

        // ASSERT - First execution (missing snapshot) should return FALSE
        assertFalse((Boolean) configNode.getCalculatedResult());
    }

    @Test
    @DisplayName("should handle empty input nodes list")
    void testCalculate_WithEmptyInputNodes_ShouldReturnTrue() {
        // ARRANGE
        configNode.setInputNodes(new ArrayList<>());

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertTrue((Boolean) configNode.getCalculatedResult());
    }

    @Test
    @DisplayName("should handle null input nodes")
    void testCalculate_WithNullInputNodes_ShouldReturnTrue() {
        // ARRANGE
        configNode.setInputNodes(null);

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertTrue((Boolean) configNode.getCalculatedResult());
    }

    @Test
    @DisplayName("should filter non-ProviderNode inputs")
    void testCalculate_WithMixedInputTypes_ShouldOnlyProcessProviderNodes()  {
        // ARRANGE
        ConstantNode nonProviderNode = new ConstantNode("constant", "test_value");
        configNode.setInputNodes(Arrays.asList(providerNode1, nonProviderNode, providerNode2));

        // ACT
        configNode.calculate(dataContext);

        // ASSERT - First execution should return FALSE
        assertFalse((Boolean) configNode.getCalculatedResult());
        assertEquals(2, configNode.getNewSnapshotData().size()); // Only ProviderNodes processed
    }


    @Test
    @DisplayName("should preserve old entry timestamp when value unchanged")
    void testCalculate_WhenValueUnchanged_ShouldPreserveOldTimestamp() {
        // ARRANGE
        long oldTimestamp = 5000L;
        configNode.setTestTime(10000L);

        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        oldSnapshot.put("source.system1.value1", new SnapshotEntry("test_value_1", oldTimestamp));

        dataContext.put("__snapshot", oldSnapshot);

        // ACT
        configNode.calculate(dataContext);

        // ASSERT
        assertEquals(oldTimestamp, configNode.getNewSnapshotData().get("source.system1.value1").timestamp());
    }
}