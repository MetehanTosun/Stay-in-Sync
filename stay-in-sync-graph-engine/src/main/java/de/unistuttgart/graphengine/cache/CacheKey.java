package de.unistuttgart.graphengine.cache;

import de.unistuttgart.graphengine.exception.GraphConstructionException;

/**
 * Cache key for storing and retrieving {@link StatefulLogicGraph} instances.
 * <p>
 * The key consists of two components:
 * <ul>
 *   <li><b>transformationId</b>: The unique identifier of the transformation rule</li>
 *   <li><b>graphHash</b>: A SHA-256 hash of the graph structure</li>
 * </ul>
 * <p>
 * This composite key ensures efficient caching while guaranteeing that structural
 * changes to a graph result in new cache entries. When a transformation rule's graph
 * is modified, the hash changes, invalidating the old cache entry and forcing
 * creation of a new {@link StatefulLogicGraph} instance.
 * <p>
 * <b>Example:</b>
 * <pre>
 * CacheKey key = new CacheKey(123L, "a3f4b2c1...");
 * StatefulLogicGraph graph = cache.get(key);
 * </pre>
 *
 * @param transformationId The unique identifier of the transformation rule.
 *                        This ID remains constant even when the graph changes.
 * @param graphHash       The SHA-256 hash (in hexadecimal format) of the graph structure.
 *                        This hash changes whenever the graph structure is modified.
 * 
 * @see GraphHasher
 * @see GraphInstanceCache
 * @see StatefulLogicGraph
 */
public record CacheKey(Long transformationId, String graphHash) {
    
    /**
     * Compact constructor that validates the inputs.
     *
     * @throws GraphConstructionException if transformationId is null or graphHash is null/blank.
     */
    public CacheKey {
        if (transformationId == null) {
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.NULL_INPUT,
                "transformationId cannot be null"
            );
        }
        if (graphHash == null || graphHash.isBlank()) {
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.NULL_INPUT,
                "graphHash cannot be null or blank"
            );
        }
    }
}
