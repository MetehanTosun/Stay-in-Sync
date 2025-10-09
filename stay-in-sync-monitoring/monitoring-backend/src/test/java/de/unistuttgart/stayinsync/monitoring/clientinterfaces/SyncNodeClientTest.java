package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SyncNodeClient.
 *
 * This class tests basic behavior of SyncNodeClient methods
 * without making actual HTTP calls.
 */
class SyncNodeClientTest {

    /**
     * Test that SyncNodeClient can be instantiated with a valid URI
     * and does not throw an exception.
     */
    @Test
    void testClientInitialization() {
        assertDoesNotThrow(() -> {
            SyncNodeClient client = new SyncNodeClient("http://localhost");
            assertNotNull(client, "SyncNodeClient instance should not be null");
        });
    }

    /**
     * Test that getLatestAll() returns a non-null value.
     *
     * Since we are using a dummy URI, we mainly verify that the method
     * does not throw and returns a value (even if it is empty).
     */
    @Test
    void testGetLatestAll_ReturnsNonNull() {
        SyncNodeClient client = new SyncNodeClient("http://localhost");

        assertDoesNotThrow(() -> {
            var result = client.getLatestAll();
            assertNotNull(result, "getLatestAll() should not return null");
        });
    }

    /**
     * Test that getLatestAll() returns the expected type.
     *
     * Adapt this assertion if getLatestAll() returns a specific type
     * (e.g., String, List, or custom object).
     */
    @Test
    void testGetLatestAll_ReturnsExpectedType() {
        SyncNodeClient client = new SyncNodeClient("http://localhost");

        var result = client.getLatestAll();
        assertNotNull(result, "Result should not be null");

        // Replace Object.class with the actual expected type if known
        assertTrue(result instanceof String, "Result should be of type String");
    }

    /**
     * Test that multiple calls to getLatestAll() do not throw exceptions.
     * This simulates repeated access, as might occur in real usage.
     */
    @Test
    void testGetLatestAll_MultipleCalls() {
        SyncNodeClient client = new SyncNodeClient("http://localhost");

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> {
                var result = client.getLatestAll();
                assertNotNull(result, "Result of call " + (i + 1) + " should not be null");
            });
        }
    }
}



