package de.unistuttgart.stayinsync.syncnode.syncjob.assets;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * An application-scoped, in-memory cache for storing API responses during a transformation execution.
 * <p>
 * The purpose of this cache is to store the results of "check" GET requests made to external
 * systems within the context of a single transformation. This prevents redundant API calls
 * and ensures that different steps within the same transformation operate on a consistent
 * set of data.
 * <p>
 * The cache is implemented to be thread-safe to handle concurrent access from different
 * processing steps. Data is held in memory for the application's lifecycle and is not persisted.
 */
@ApplicationScoped
public class CheckResponseCacheService {

    /**
     * The primary data structure of the cache.
     * The outer key is the {@code transformationId}.
     * The value is an encapsulation object that holds the responses for that transformation.
     */
    private final Map<Long, TransformationCacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Adds a successful GET response body to the cache.
     * <p>
     * This method is thread-safe. It retrieves or creates the cache entry for the given
     * {@code transformationId} as needed and delegates the addition of the response to that entry.
     *
     * @param transformationId The ID of the transformation to which the response belongs. Cannot be null.
     * @param targetArcId      The ID of the target ARC used for the API call. Cannot be null.
     * @param responseBody     The response body from the GET call as a String. Cannot be null.
     */
    public void addResponse(Long transformationId, Long targetArcId, String responseBody) {
        // Parameter validation for robustness.
        Objects.requireNonNull(transformationId, "transformationId must not be null");
        Objects.requireNonNull(targetArcId, "targetArcId must not be null");
        Objects.requireNonNull(responseBody, "responseBody must not be null");

        // Atomically get or create the TransformationCacheEntry for the transformation.
        TransformationCacheEntry entry = cache.computeIfAbsent(transformationId, k -> {
            Log.debugf("Creating new cache entry for transformation ID: %d", k);
            return new TransformationCacheEntry();
        });

        // Delegate the actual add operation to the entry object.
        entry.add(targetArcId, responseBody);
    }

    /**
     * Retrieves a read-only copy of all cached responses for a given transformation.
     * <p>
     * This method returns a snapshot of the data. The internal sets are converted to lists,
     * which is suitable for JSON serialization and API consumers. Any modifications to the
     * returned map or lists will not affect the cache itself.
     *
     * @param transformationId The ID of the transformation whose responses are to be retrieved.
     * @return An {@link Optional} containing a map of responses (Target ARC ID -> List of bodies),
     *         or an empty Optional if no entry was found for the given ID.
     */
    public Optional<Map<Long, List<String>>> getResponsesByTransformationId(Long transformationId) {
        return Optional.ofNullable(cache.get(transformationId))
                .map(TransformationCacheEntry::getResponsesAsSnapshot);
    }

    /**
     * An internal class that encapsulates the cached responses for a single transformation.
     */
    private static class TransformationCacheEntry {
        /**
         * Stores responses per target ARC.
         * Key: {@code targetArcId}
         * Value: A set of unique response bodies. CopyOnWriteArraySet is optimized for
         *        "read-mostly" scenarios in concurrent environments.
         */
        private final Map<Long, Set<String>> responsesByArc = new ConcurrentHashMap<>();

        /**
         * Adds a response body for a specific target ARC.
         */
        void add(Long targetArcId, String responseBody) {
            Set<String> responseSet = responsesByArc.computeIfAbsent(targetArcId, k -> new CopyOnWriteArraySet<>());
            responseSet.add(responseBody);
        }

        /**
         * Creates and returns a copy of the stored data.
         * Converting sets to lists is part of the public contract of this class.
         */
        Map<Long, List<String>> getResponsesAsSnapshot() {
            return this.responsesByArc.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> List.copyOf(entry.getValue())
                    ));
        }
    }
}