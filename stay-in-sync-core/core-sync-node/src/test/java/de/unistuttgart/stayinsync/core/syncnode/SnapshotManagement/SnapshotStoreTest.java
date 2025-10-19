package de.unistuttgart.stayinsync.core.syncnode.SnapshotManagement;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.unistuttgart.stayinsync.syncnode.SnapshotManagement.SnapshotStore;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.TransformationResultDTO;

/**
 * Unit tests for {@link SnapshotStore}.
 * <p>
 * Verifies correct indexing, retrieval, TTL-based eviction, and history size
 * management for snapshot data.
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
public class SnapshotStoreTest {

    private SnapshotStore store;

    @BeforeEach
    void setup() {
        store = new SnapshotStore();
    }

    private SnapshotDTO makeSnapshot(String id, Long transId, Instant createdAt) {
        SnapshotDTO dto = new SnapshotDTO();
        dto.setSnapshotId(id);
        dto.setCreatedAt(createdAt);
        if (transId != null) {
            TransformationResultDTO tr = new TransformationResultDTO();
            tr.setTransformationId(transId);
            dto.setTransformationResult(tr);
        }
        return dto;
    }

    @Test
    @DisplayName("put(): stores snapshot and updates indices")
    void put_storesAndIndexes() {
        SnapshotDTO s = makeSnapshot("a", 1L, Instant.now());
        store.put(s);

        // Should be retrievable
        Optional<SnapshotDTO> found = store.getBySnapshotId("a");
        assertTrue(found.isPresent());
        assertSame(s, found.get());

        // Should be the latest for transformation 1
        Optional<SnapshotDTO> latest = store.getLatestByTransformationId(1L);
        assertTrue(latest.isPresent());
        assertSame(s, latest.get());

        // Should appear in history list
        List<SnapshotDTO> list = store.listByTransformationId(1L, 5);
        assertEquals(1, list.size());
        assertSame(s, list.get(0));
    }

    @Test
    @DisplayName("listByTransformationId(): respects limit and ordering (newest first) and history limit")
    void list_respectsLimitAndOrder_andHistoryLimit() {
        Instant base = Instant.now();
        // Insert 6 snapshots; HISTORY_LIMIT is 5 so the oldest should be trimmed
        for (int i = 0; i < 6; i++) {
            SnapshotDTO s = makeSnapshot("id" + i, 1L, base.plusSeconds(i));
            store.put(s);
        }
        // Full list (limit > HISTORY_LIMIT) still returns only 5, newest first
        List<SnapshotDTO> list = store.listByTransformationId(1L, 10);
        assertEquals(5, list.size());
        // Check ordering: index 0 should be the newest timestamp
        assertTrue(list.get(0).getCreatedAt().isAfter(list.get(4).getCreatedAt()));

        // Limit 2 should return only the first two (newest two)
        List<SnapshotDTO> top2 = store.listByTransformationId(1L, 2);
        assertEquals(2, top2.size());
        assertTrue(top2.get(0).getCreatedAt().isAfter(top2.get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("getBySnapshotId(): returns empty when not found")
    void getBySnapshotId_notFound() {
        assertTrue(store.getBySnapshotId("none").isEmpty());
    }

    @Test
    @DisplayName("getLatestByTransformationId(): returns empty when not present")
    void getLatestByTransformationId_empty() {
        assertTrue(store.getLatestByTransformationId(999L).isEmpty());
    }

    @Test
    @DisplayName("getLatestByAllTransformationIds(): returns shallow copy")
    void getLatestByAllTransformationIds_returnsCopy() {
        SnapshotDTO s = makeSnapshot("abc", 10L, Instant.now());
        store.put(s);
        Map<Long, SnapshotDTO> map = store.getLatestByAllTransformationIds();
        assertEquals(1, map.size());
        assertTrue(map.containsKey(10L));

        // modify returned map, should not affect internal map
        map.clear();
        assertEquals(1, store.getLatestByAllTransformationIds().size());
    }

    @Test
    @DisplayName("evictExpired(): removes old snapshots beyond TTL")
    void evictExpired_removesOldEntries() {
        // Create a snapshot older than TTL (older than 24h)
        Instant old = Instant.now().minusSeconds(25 * 60 * 60); // 25h
        SnapshotDTO expired = makeSnapshot("old", 1L, old);
        store.put(expired);

        // Force eviction indirectly via public calls
        store.getBySnapshotId("old");

        assertTrue(store.getBySnapshotId("old").isEmpty(), "Expired snapshot should be evicted");
        assertTrue(store.getLatestByTransformationId(1L).isEmpty(), "Expired latest snapshot should be evicted");
        assertTrue(store.listByTransformationId(1L, 5).isEmpty(), "Expired entries should be purged from history");
    }

    @Test
    @DisplayName("put(): ignores null input")
    void put_nullIgnored() {
        assertDoesNotThrow(() -> store.put(null));
    }
}
