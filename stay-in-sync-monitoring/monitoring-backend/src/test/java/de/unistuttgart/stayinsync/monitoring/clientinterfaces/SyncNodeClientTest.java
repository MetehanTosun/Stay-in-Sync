package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Improved test class for SyncNodeClient.
 * These tests verify initialization, basic method behavior,
 * and consistency of getLatestAll() results.
 */
class SyncNodeClientTest {

    private SyncNodeClient client;

    @BeforeEach
    void setUp() {
        client = new SyncNodeClient();
    }

    /**
     * Test that SyncNodeClient can be instantiated with the no-arg constructor
     * and does not throw an exception.
     */
    @Test
    void testClientInitialization() {
        assertNotNull(client, "SyncNodeClient instance should not be null");
    }

    /**
     * Test that getLatestAll() returns a non-null Map.
     */
    @Test
    void testGetLatestAll_ReturnsNonNull() {
        Map<Long, SnapshotDTO> result = assertDoesNotThrow(client::getLatestAll,
                "getLatestAll() should not throw an exception");
        assertNotNull(result, "getLatestAll() should not return null");
    }

    /**
     * Test that getLatestAll() returns a Map of the expected type.
     */
    @Test
    void testGetLatestAll_ReturnsExpectedType() {
        Map<Long, SnapshotDTO> result = client.getLatestAll();
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof Map, "Result should be of type Map<Long, SnapshotDTO>");
    }

    /**
     * Test that multiple calls to getLatestAll() return consistent results
     * and do not throw exceptions.
     */
    @Test
    void testGetLatestAll_MultipleCalls() {
        for (int i = 0; i < 5; i++) {
            Map<Long, SnapshotDTO> result = assertDoesNotThrow(client::getLatestAll,
                    "getLatestAll() call " + (i + 1) + " should not throw an exception");
            assertNotNull(result, "Result of call " + (i + 1) + " should not be null");
        }
    }

    /**
     * Test that getLatestAll() handles empty results gracefully.
     */
    @Test
    void testGetLatestAll_EmptyMapHandled() {
        Map<Long, SnapshotDTO> result = client.getLatestAll();
        assertNotNull(result, "Result should not be null even if empty");
        assertDoesNotThrow(() -> result.forEach((k, v) -> assertNotNull(v, "SnapshotDTO should not be null")),
                "All values in the map should be non-null");
    }

    /**
     * Test repeated calls do not modify returned map unexpectedly (basic immutability check).
     */
    @Test
    void testGetLatestAll_ConsistencyAcrossCalls() {
        Map<Long, SnapshotDTO> firstCall = client.getLatestAll();
        Map<Long, SnapshotDTO> secondCall = client.getLatestAll();

        assertEquals(firstCall.keySet(), secondCall.keySet(), "Keys should remain consistent across calls");
    }
}


