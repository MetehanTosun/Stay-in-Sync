package de.unistuttgart.stayinsync.syncnode.LogicGraph;

import de.unistuttgart.graphengine.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class GraphInstanceCache {

    private final Map<Long, StatefulLogicGraph> cache = new ConcurrentHashMap<>();


    /**
     * Retrieves an instance from the cache.
     * If it doesn't exist, it is created from the provided graph definition and then stored in the cache.
     */
    public StatefulLogicGraph getOrCreate(long transformationId, List<Node> graphDefinition) {
        // computeIfAbsent ensures the instance is created only once
        return cache.computeIfAbsent(transformationId, id -> new StatefulLogicGraph(graphDefinition));
    }

    public void remove(long transformationId) {
        cache.remove(transformationId);
    }

    public void invalidate(long transformationId) {
        cache.remove(transformationId);
    }
}