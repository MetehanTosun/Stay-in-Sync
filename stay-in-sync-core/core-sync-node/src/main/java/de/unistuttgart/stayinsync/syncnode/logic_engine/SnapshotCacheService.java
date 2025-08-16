package de.unistuttgart.stayinsync.syncnode.logic_engine;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory, application-scoped cache for storing change-detection snapshots.
 * <p>
 * This service provides a thread-safe, volatile storage for the last known state
 * of a transformation rule's monitored values.
 */
@ApplicationScoped
public class SnapshotCacheService {

    /**
     * The underlying concurrent map that stores the cached snapshots.
     * The key is the unique ID of the {@code TransformationRule}, and the value is the
     * serialized JSON string of the snapshot data (e.g., a map of SnapshotEntry objects).
     * {@link ConcurrentHashMap} is used to ensure thread-safe access from multiple
     * concurrent job executions.
     */
    private final Map<Long, String> snapshotCache = new ConcurrentHashMap<>();

    /**
     * Retrieves a snapshot from the cache for a given transformation rule ID.
     *
     * @param transformationRuleId The unique ID of the transformation rule.
     * @return An {@link Optional} containing the snapshot's JSON string if it exists,
     * or an empty {@link Optional} if no snapshot is found for the given ID.
     */
    public Optional<String> getSnapshot(Long transformationRuleId) {
        return Optional.ofNullable(snapshotCache.get(transformationRuleId));
    }

    /**
     * Saves or updates a snapshot in the cache.
     * If a snapshot for the given ID already exists, it will be overwritten.
     *
     * @param transformationRuleId The unique ID of the transformation rule.
     * @param snapshotJson         The snapshot data, serialized as a JSON string.
     */
    public void saveSnapshot(Long transformationRuleId, String snapshotJson) {
        snapshotCache.put(transformationRuleId, snapshotJson);
    }
}
