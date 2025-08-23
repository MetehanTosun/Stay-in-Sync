package de.unistuttgart.stayinsync.logic_engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.syncnode.logic_engine.SnapshotCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SnapshotCacheService.
 * This class verifies the core functionality of the in-memory cache.
 */
public class SnapshotCacheServiceTest {

    private SnapshotCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Create a new, fresh cache instance before each test
        cacheService = new SnapshotCacheService();
    }

    @Test
    void testSaveAndGetSnapshot_ShouldReturnSameValue() throws IOException {
        // ARRANGE
        long transformationId = 1L;
        JsonNode snapshotToSave = objectMapper.readTree("{\"value\": \"test_data\"}");

        // ACT
        cacheService.saveSnapshot(transformationId, snapshotToSave);
        Optional<JsonNode> retrievedSnapshotOpt = cacheService.getSnapshot(transformationId);

        // ASSERT
        assertTrue(retrievedSnapshotOpt.isPresent(), "Snapshot should be present in the cache.");
        assertEquals(snapshotToSave, retrievedSnapshotOpt.get(), "Retrieved snapshot should be identical to the saved one.");
    }

    @Test
    void testGetSnapshot_WhenKeyDoesNotExist_ShouldReturnEmptyOptional() {
        // ARRANGE
        long nonExistentId = 999L;

        // ACT
        Optional<JsonNode> result = cacheService.getSnapshot(nonExistentId);

        // ASSERT
        assertTrue(result.isEmpty(), "Getting a non-existent key should return an empty Optional.");
    }

    @Test
    void testSaveSnapshot_WhenKeyExists_ShouldOverwriteValue() throws IOException {
        // ARRANGE
        long transformationId = 1L;
        JsonNode firstSnapshot = objectMapper.readTree("{\"value\": \"first_version\"}");
        JsonNode secondSnapshot = objectMapper.readTree("{\"value\": \"second_version\"}");

        // ACT
        cacheService.saveSnapshot(transformationId, firstSnapshot); // Save the first version
        cacheService.saveSnapshot(transformationId, secondSnapshot); // Overwrite with the second version

        Optional<JsonNode> retrievedSnapshotOpt = cacheService.getSnapshot(transformationId);

        // ASSERT
        assertTrue(retrievedSnapshotOpt.isPresent());
        assertEquals(secondSnapshot, retrievedSnapshotOpt.get(), "The snapshot should be overwritten with the newer version.");
    }

    @Test
    void testCache_ShouldHandleMultipleKeysCorrectly() throws IOException {
        // ARRANGE
        long id1 = 1L;
        JsonNode snapshot1 = objectMapper.readTree("{\"value\": \"data_for_1\"}");
        long id2 = 2L;
        JsonNode snapshot2 = objectMapper.readTree("{\"value\": \"data_for_2\"}");

        // ACT
        cacheService.saveSnapshot(id1, snapshot1);
        cacheService.saveSnapshot(id2, snapshot2);

        // ASSERT
        assertEquals(snapshot1, cacheService.getSnapshot(id1).get(), "Should retrieve correct data for ID 1.");
        assertEquals(snapshot2, cacheService.getSnapshot(id2).get(), "Should retrieve correct data for ID 2.");
    }
}
