package de.unistuttgart.stayinsync.syncnode.LogicGraph;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@ApplicationScoped
public class GraphHasher {

    @Inject
    ObjectMapper objectMapper;

    /**
     * Generates a stable SHA-256 hash from a graph structure.
     * @param graphNodes The list of nodes representing the graph.
     * @return A hexadecimal string representing the hash of the graph.
     */
    public String hash(List<Node> graphNodes) {
        try {
            String graphAsString = objectMapper.writeValueAsString(graphNodes);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(graphAsString.getBytes("UTF-8"));

            return HexFormat.of().formatHex(hashBytes);

        } catch (Exception e) {
            return String.valueOf(graphNodes.hashCode());
        }
    }
}
