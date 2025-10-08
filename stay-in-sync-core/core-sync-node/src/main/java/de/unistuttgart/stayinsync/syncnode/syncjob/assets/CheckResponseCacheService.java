package de.unistuttgart.stayinsync.syncnode.syncjob.assets;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@ApplicationScoped
public class CheckResponseCacheService {

    private final Map<Long, Map<Long, Set<String>>> cache = new ConcurrentHashMap<>();

    /**
     * Adds a successful GET response body to the cache.
     * This method is thread-safe and will create nested maps/lists as needed.
     *
     * @param transformationId The ID of the transformation being executed.
     * @param targetArcId The ID of the target ARC that was used during api calls.
     * @param responseBody The response body from the GET call as String.
     */
    public void addResponse(Long transformationId, Long targetArcId, String responseBody) {
        Map<Long, Set<String>> arcMap = cache.computeIfAbsent(transformationId, k -> new ConcurrentHashMap<>());
        Set<String> responseSet = arcMap.computeIfAbsent(targetArcId, k -> new CopyOnWriteArraySet<>());
        responseSet.add(responseBody);
    }

    /**
     * Retrieves all cached responses for a given transformation, converting Sets to Lists for the DTO.
     *
     * @param transformationId The ID of the transformation to look up.
     * @return An Optional containing the map of responses (Target ARC ID -> List of bodies), or empty if not found.
     */
    public Optional<Map<Long, List<String>>> getResponsesByTransformationId(Long transformationId) {
        Map<Long, Set<String>> arcMap = cache.get(transformationId);
        if (arcMap == null) {
            return Optional.empty();
        }

        Map<Long, List<String>> result = arcMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new CopyOnWriteArrayList<>(entry.getValue())
                ));

        return Optional.of(result);
    }
}
