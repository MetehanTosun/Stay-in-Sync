package de.unistuttgart.stayinsync.syncnode.SnapshotManagement;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.TransformationResultDTO;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory store for
 * {@link de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO}
 * used by the Snapshot Management & Replay feature.
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Index snapshots by snapshot id and by transformation id.</li>
 * <li>Keep the latest snapshot per transformation for quick lookup.</li>
 * <li>Maintain a bounded per-transformation history (deque) for recent
 * items.</li>
 * <li>Evict entries beyond a time-to-live (TTL) to cap memory usage.</li>
 * </ul>
 * This store is intentionally simple and thread-safe via concurrent collections
 * since
 * it lives in a single application node. Persistence/durability is out of scope
 * here
 * and handled elsewhere when required by the system.
 *
 * @author Mohammed-Ammar Hassnou
 */

@ApplicationScoped
public class SnapshotStore {

    // Default TTL for snapshots (seconds). Entries older than this are evicted. 24h
    private static final long DEFAULT_TTL_SECONDS = 24 * 60 * 60;
    // Maximum number of recent snapshots to keep per transformation in memory. (you
    // can modify this accordingly)
    private static final int HISTORY_LIMIT = 5;
    // Primary index: snapshotId -> snapshot.
    private final Map<String, SnapshotDTO> bySnapshotId = new ConcurrentHashMap<>();
    // Fast path: latest snapshot for a given transformation id.
    private final Map<Long, SnapshotDTO> latestByTransformationId = new ConcurrentHashMap<>();
    // Bounded deque of recent snapshots per transformation.
    private final Map<Long, Deque<SnapshotDTO>> historyByTransformationId = new ConcurrentHashMap<>();

    /**
     * Insert a snapshot into all relevant indices and enforce retention policies.
     * <p>
     * If the snapshot contains a {@link TransformationResultDTO} with a non-null
     * transformation id, it becomes the latest for that id and is added at the
     * front of the per-transformation history. Excess history entries are truncated
     * from the tail to respect {@link #HISTORY_LIMIT}.
     * </p>
     * The method also triggers a best-effort eviction pass for expired entries.
     *
     * @param snapshot the snapshot to store; ignored if {@code null}
     */
    public void put(SnapshotDTO snapshot) {
        // Ignore null inputs for defensive programming.
        if (snapshot == null)
            return;
        // Index by unique snapshot id for direct retrieval.
        bySnapshotId.put(snapshot.getSnapshotId(), snapshot);

        TransformationResultDTO transformationResult = snapshot.getTransformationResult();
        // Maintain latest/indexed history only if we have a transformation id.
        if (transformationResult != null && transformationResult.getTransformationId() != null) {
            Long transformationId = transformationResult.getTransformationId();
            latestByTransformationId.put(transformationId, snapshot);
            // Ensure a deque exists for this transformation.
            historyByTransformationId.computeIfAbsent(transformationId, k -> new ArrayDeque<>(HISTORY_LIMIT));
            Deque<SnapshotDTO> dq = historyByTransformationId.get(transformationId);
            // Newest snapshots go to the front (head) of the deque.
            dq.addFirst(snapshot);
            // Trim the deque to the configured history size.
            while (dq.size() > HISTORY_LIMIT)
                dq.removeLast();
        }
        // Opportunistic cleanup of outdated entries.
        evictExpired();
    }

    /**
     * Retrieve a snapshot by its unique id.
     *
     * @param snapshotId the snapshot identifier
     * @return an {@link Optional} containing the snapshot if present and not
     *         expired
     */
    public Optional<SnapshotDTO> getBySnapshotId(String snapshotId) {
        evictExpired();
        return Optional.ofNullable(bySnapshotId.get(snapshotId));
    }

    /**
     * Get the most recent snapshot for a given transformation id.
     *
     * @param transformationId the transformation identifier
     * @return an {@link Optional} with the latest snapshot if present and not
     *         expired
     */
    public Optional<SnapshotDTO> getLatestByTransformationId(Long transformationId) {
        evictExpired();
        return Optional.ofNullable(latestByTransformationId.get(transformationId));
    }

    /**
     * List up to {@code limit} recent snapshots for a transformation, ordered
     * from newest to oldest.
     *
     * @param transformationId the transformation identifier
     * @param limit            maximum number of items to return; values <= 0 result
     *                         in an empty list
     * @return an immutable list view of the recent snapshots (may be empty)
     */
    public List<SnapshotDTO> listByTransformationId(Long transformationId, int limit) {
        evictExpired();
        // Look up the bounded deque for this transformation.
        Deque<SnapshotDTO> dq = historyByTransformationId.get(transformationId);
        if (dq == null)
            return List.of();
        List<SnapshotDTO> out = new ArrayList<>(Math.min(limit, dq.size()));
        int i = 0;
        // Iterate from newest to oldest, copying up to the requested limit.
        for (SnapshotDTO s : dq) {
            if (i++ >= limit)
                break;
            out.add(s);
        }
        return out;
    }

    /**
     * Remove snapshots older than {@link #DEFAULT_TTL_SECONDS} from all indices.
     * <p>
     * This is a best-effort pass; callers may still race with concurrent puts/gets,
     * but the use of concurrent collections ensures thread safety.
     * </p>
     */
    private void evictExpired() {
        // Current time (epoch seconds) for TTL comparison.
        long now = Instant.now().getEpochSecond();

        // Remove expired entries from bySnapshotId.
        bySnapshotId.values().removeIf(s -> isExpired(s, now));
        // Remove expired entries from latestByTransformationId.
        latestByTransformationId.values().removeIf(s -> isExpired(s, now));
        // Remove expired entries from per-transformation history deques.
        historyByTransformationId.values().forEach(dq -> dq.removeIf(s -> isExpired(s, now)));
        // Drop empty history buckets to free memory.
        historyByTransformationId.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Determine whether the snapshot's age exceeds the configured TTL.
     *
     * @param s           the snapshot to test
     * @param nowEpochSec the current time in epoch seconds
     * @return {@code true} if expired; {@code false} otherwise
     */
    private boolean isExpired(SnapshotDTO s, long nowEpochSec) {
        return s.getCreatedAt() != null &&
                (nowEpochSec - s.getCreatedAt().getEpochSecond()) > DEFAULT_TTL_SECONDS;
    }

    /**
     * Return a shallow copy of the latest snapshot map across all transformation
     * ids.
     *
     * @return a new {@link HashMap} mapping transformation id -> latest snapshot
     */
    public Map<Long, SnapshotDTO> getLatestByAllTransformationIds() {
        evictExpired();
        return new HashMap<>(latestByTransformationId);
    }

}