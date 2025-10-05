package de.unistuttgart.stayinsync.syncnode.LogicGraph;

import de.unistuttgart.graphengine.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class GraphInstanceCache {

    private final Map<CacheKey, StatefulLogicGraph> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves an instance from the cache.
     * If it doesn't exist, it is created from the provided graph definition and then stored in the cache.
     */
    public StatefulLogicGraph getOrCreate(long transformationId, String graphHash, List<Node> graphDefinition) {
        CacheKey key = new CacheKey(transformationId, graphHash);
        return cache.computeIfAbsent(key, k -> new StatefulLogicGraph(graphDefinition));
    }

    /**
     * Removes all cached entries associated with a given transformationId,
     * regardless of their graph hash. This is useful when a rule is deleted.
     */
    public void remove(long transformationId) {
        cache.keySet().removeIf(key -> key.transformationId().equals(transformationId));
    }
}