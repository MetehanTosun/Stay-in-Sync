package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SyncNodeClient.
 *
 * This class tests basic behavior of SyncNodeClient methods
 * without making actual HTTP calls.
 */
class SyncNodeClientTest {

    /**
     * Test that SyncNodeClient can be instantiated with the no-arg constructor
     * and does not throw an exception.
     */
    @Test
    void testClientInitialization() {
        assertDoesNotThrow(() -> {
            SyncNodeClient client = new SyncNodeClient();
            assertNotNull(client, "SyncNodeClient instance should not be null");
        });
    }

    /**
     * Test that getLatestAll() returns a non-null Map.
     */
    @Test
    void testGetLatestAll_ReturnsNonNull() {
        SyncNodeClient client = new SyncNodeClient();

        assertDoesNotThrow(() -> {
            Map<Long, SnapshotDTO> result = client.getLatestAll();
            assertNotNull(result, "getLatestAll() should not return null");
        });
    }

    /**
     * Test that getLatestAll() returns the expected Map type.
     */
    @Test
    void testGetLatestAll_ReturnsExpectedType() {
        SyncNodeClient client = new SyncNodeClient();

        Map<Long, SnapshotDTO> result = client.getLatestAll();
        assertNotNull(result, "Result should not be null");
        assertInstanceOf(Map.class, result, "Result should be of type Map<Long, SnapshotDTO>");
    }

    /**
     * Test that multiple calls to getLatestAll() do not throw exceptions.
     */
    @Test
    void testGetLatestAll_MultipleCalls() {
        SyncNodeClient client = new SyncNodeClient();

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            assertDoesNotThrow(() -> {
                Map<Long, SnapshotDTO> result = client.getLatestAll();
                assertNotNull(result, "Result of call " + (finalI + 1) + " should not be null");
            });
        }
    }
}


