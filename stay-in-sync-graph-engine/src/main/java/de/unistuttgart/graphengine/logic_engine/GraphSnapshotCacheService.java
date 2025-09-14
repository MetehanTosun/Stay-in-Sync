package de.unistuttgart.graphengine.logic_engine;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory, application-scoped cache for storing change-detection snapshots.
 * <p>
 * This service provides a thread-safe, volatile storage for the last known state
 * of a transformation's monitored values. The data is stored directly on the heap
 * of the syncnode application and will be lost upon restart.
 */
@ApplicationScoped
public class GraphSnapshotCacheService {

    /**
     * The underlying concurrent map that stores the cached snapshots.
     * The key is the unique ID of the transformation, and the value is the
     * snapshot data as a {@link JsonNode} to avoid repeated serialization.
     * {@link ConcurrentHashMap} is used to ensure thread-safe access from multiple
     * concurrent job executions.
     */
    private final Map<Long, JsonNode> snapshotCache = new ConcurrentHashMap<>();

    /**
     * Retrieves a snapshot from the cache for a given transformation ID.
     *
     * @param transformationId The unique ID of the transformation.
     * @return An {@link Optional} containing the snapshot's {@link JsonNode} if it exists,
     * or an empty {@link Optional} if no snapshot is found for the given ID.
     */
    public Optional<JsonNode> getSnapshot(long transformationId) {
        return Optional.ofNullable(snapshotCache.get(transformationId));
    }

    /**
     * Saves or updates a snapshot in the cache.
     * If a snapshot for the given ID already exists, it will be overwritten.
     *
     * @param transformationId The unique ID of the transformation.
     * @param snapshotNode     The snapshot data as a {@link JsonNode}.
     */
    public void saveSnapshot(long transformationId, JsonNode snapshotNode) {
        snapshotCache.put(transformationId, snapshotNode);
    }
}