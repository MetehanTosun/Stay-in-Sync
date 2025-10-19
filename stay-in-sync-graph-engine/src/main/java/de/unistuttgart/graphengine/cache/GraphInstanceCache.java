package de.unistuttgart.graphengine.cache;

import de.unistuttgart.graphengine.exception.GraphConstructionException;
import de.unistuttgart.graphengine.nodes.Node;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache for {@link StatefulLogicGraph} instances.
 * <p>
 * This cache stores graph instances by their {@link CacheKey}, which combines
 * the transformation ID with a hash of the graph structure. This ensures that:
 * <ul>
 *   <li>Multiple transformations can coexist in the cache</li>
 *   <li>Graph structure changes automatically invalidate old instances</li>
 *   <li>The same graph structure is reused across evaluations (with its state)</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> This cache uses a {@link ConcurrentHashMap}, making it safe
 * for concurrent access from multiple threads. The {@code computeIfAbsent} operation
 * is atomic.
 * <p>
 * <b>Memory Management:</b> Old graph instances are automatically discarded when
 * the graph structure changes (different hash). Explicit removal via {@link #remove(long)}
 * should be called when a transformation rule is deleted.
 *
 * @see StatefulLogicGraph
 * @see CacheKey
 * @see GraphHasher
 */
@ApplicationScoped
public class GraphInstanceCache {

    private final Map<CacheKey, StatefulLogicGraph> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves a cached graph instance or creates a new one if not present.
     * <p>
     * This operation is atomic - if multiple threads request the same key simultaneously,
     * only one will create the graph instance.
     * <p>
     * <b>Cache Hit:</b> Returns the existing instance with its preserved state (snapshot).
     * <b>Cache Miss:</b> Creates a new instance and stores it for future use.
     *
     * @param transformationId The unique identifier of the transformation rule.
     * @param graphHash        The SHA-256 hash of the current graph structure.
     * @param graphDefinition  The list of nodes representing the graph. Only used if
     *                        the cache misses (new instance needs to be created).
     * @return The cached or newly created {@link StatefulLogicGraph} instance.
     * @throws GraphConstructionException if any parameter is invalid or the graph structure is invalid
     *                                   (only on cache miss when creating new instance).
     */
    public StatefulLogicGraph getOrCreate(long transformationId, String graphHash, List<Node> graphDefinition) {
        if (graphHash == null || graphHash.isBlank()) {
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.NULL_INPUT,
                "graphHash cannot be null or blank"
            );
        }
        if (graphDefinition == null) {
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.NULL_INPUT,
                "graphDefinition cannot be null"
            );
        }

        CacheKey key = new CacheKey(transformationId, graphHash);
        
        // Check if we have a cache hit
        if (cache.containsKey(key)) {
            Log.tracef("Cache HIT for transformation %d with hash %s", 
                transformationId, graphHash.substring(0, Math.min(16, graphHash.length())) + "...");
        } else {
            Log.debugf("Cache MISS for transformation %d with hash %s. Creating new instance.",
                transformationId, graphHash.substring(0, Math.min(16, graphHash.length())) + "...");
        }
        
        return cache.computeIfAbsent(key, k -> {
            Log.infof("Creating new StatefulLogicGraph for transformation %d", transformationId);
            try {
                return new StatefulLogicGraph(graphDefinition);
            } catch (GraphConstructionException e) {
                Log.errorf(e, "Failed to create StatefulLogicGraph for transformation %d: %s (ErrorType: %s)",
                    transformationId, e.getMessage(), e.getErrorType());
                throw e; // Re-throw to propagate to caller
            }
        });
    }

    /**
     * Removes all cached entries associated with a given transformation ID,
     * regardless of their graph hash.
     * <p>
     * This method should be called when a transformation rule is deleted from
     * the system to free up memory. It removes all versions of the graph
     * (different hashes due to historical changes).
     *
     * @param transformationId The unique identifier of the transformation rule to remove.
     * @return The number of cache entries that were removed.
     */
    public int remove(long transformationId) {
        int removedCount = 0;
        
        // Remove all entries for this transformation
        var keysToRemove = cache.keySet().stream()
            .filter(key -> key.transformationId().equals(transformationId))
            .toList();
        
        for (CacheKey key : keysToRemove) {
            if (cache.remove(key) != null) {
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            Log.infof("Removed %d cached graph instance(s) for transformation %d",
                removedCount, transformationId);
        } else {
            Log.debugf("No cached instances found for transformation %d", transformationId);
        }
        
        return removedCount;
    }

    /**
     * Returns the current number of cached graph instances.
     * Useful for monitoring and debugging.
     *
     * @return The total number of cached instances across all transformations.
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Clears all cached graph instances.
     * <p>
     * <b>Warning:</b> This will lose all accumulated state (snapshots) in all graphs.
     * Should only be used for testing or maintenance purposes.
     */
    public void clear() {
        int previousSize = cache.size();
        cache.clear();
        Log.warnf("Cleared entire graph cache. Removed %d instances.", previousSize);
    }
}
