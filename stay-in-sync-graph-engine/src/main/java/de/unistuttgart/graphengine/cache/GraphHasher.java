package de.unistuttgart.graphengine.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.GraphSerializationException;
import de.unistuttgart.graphengine.nodes.Node;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Service responsible for generating stable SHA-256 hashes from graph structures.
 * <p>
 * The hash is used as part of the cache key to determine if a graph's structure
 * has changed. A change in the graph structure results in a different hash,
 * which invalidates the cache and forces a new graph instance to be created.
 */
@ApplicationScoped
public class GraphHasher {

    @Inject
    ObjectMapper objectMapper;

    /**
     * Generates a stable SHA-256 hash from a graph structure.
     * <p>
     * The graph is first serialized to JSON, then hashed using SHA-256.
     * This ensures that identical graph structures always produce the same hash.
     *
     * @param graphNodes The list of nodes representing the graph.
     * @return A hexadecimal string representing the SHA-256 hash of the graph.
     * @throws GraphSerializationException if serialization or hash computation fails.
     */
    public String hash(List<Node> graphNodes) throws GraphSerializationException {
        if (graphNodes == null) {
            throw new GraphSerializationException(
                GraphSerializationException.ErrorType.INVALID_FORMAT,
                "Cannot hash null graph node list"
            );
        }

        try {
            // Serialize graph to JSON for consistent hashing
            String graphAsString = objectMapper.writeValueAsString(graphNodes);
            
            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(graphAsString.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            String hash = HexFormat.of().formatHex(hashBytes);
            
            Log.debugf("Successfully computed hash for graph with %d nodes: %s", 
                graphNodes.size(), hash.substring(0, 16) + "...");
            
            return hash;

        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialize graph for hashing: %d nodes", graphNodes.size());
            throw new GraphSerializationException(
                GraphSerializationException.ErrorType.SERIALIZATION_FAILED,
                String.format("Failed to serialize graph with %d nodes for hash computation", graphNodes.size()),
                e
            );
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available in any standard JVM
            Log.fatalf(e, "CRITICAL: SHA-256 algorithm not available on this system");
            throw new GraphSerializationException(
                GraphSerializationException.ErrorType.HASH_COMPUTATION_FAILED,
                "SHA-256 algorithm is not available on this system",
                e
            );
        }
    }
}
