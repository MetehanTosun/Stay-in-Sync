package de.unistuttgart.stayinsync.syncnode.monitor.snapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.InputDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.NodeDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphSnapshot.GraphSnapshotDTO;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.ConstantNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.FinalNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.ProviderNode;

/**
 * Builds a GraphSnapshotDTO in case of an error and contains an internal mapper
 * from runtime node list -> GraphDTO (only structure: IDs, names, positions,
 * edges).
 */
public final class SnapshotFactory {

    private SnapshotFactory() {
    }

    /**
     * Creates a snapshot exclusively in case of an error.
     *
     * @param jobId       (optional) Job ID for correlation; can be null if not
     *                    available
     * @param nodes       Runtime graph (Node objects, including input edges)
     * @param dataContext The data context used for evaluation
     * @param error       The GraphEvaluationException that occurred
     * @return populated GraphSnapshotDTO
     */
    public static GraphSnapshotDTO fromFailure(String jobId,
            List<Node> nodes,
            Map<String, JsonNode> dataContext,
            GraphEvaluationException error) {
        GraphDTO graph = mapToGraphDTO(nodes);

        GraphSnapshotDTO dto = new GraphSnapshotDTO();
        dto.setSnapshotId(UUID.randomUUID().toString());
        dto.setJobId(jobId);
        dto.setCreatedAt(Instant.now());
        dto.setGraph(graph);
        dto.setDataContext(dataContext);
        dto.setErrorTitle(error.getTitle());
        dto.setErrorMessage(error.getMessage());
        dto.setErrorType(error.getErrorType());
        return dto;
    }

    /**
     * Internal mapper: List<Node> -> GraphDTO.
     * Includes only structural information (id, name, offsetX/Y, inputNodes(id,
     * orderIndex)).
     */
    private static GraphDTO mapToGraphDTO(List<Node> nodes) {
        GraphDTO graphDTO = new GraphDTO();
        List<NodeDTO> nodeDTOs = new ArrayList<>();

        if (nodes != null) {
            for (Node n : nodes) {
                NodeDTO nd = new NodeDTO();
                nd.setId(n.getId());
                nd.setName(n.getName());
                nd.setOffsetX(n.getOffsetX());
                nd.setOffsetY(n.getOffsetY());
                nd.setNodeType(getNodeType(n));
                nd.setArcId(getArcId(n));
                nd.setValue(getValue(n));
                nd.setOperatorType(getOperatorType(n).toString()); // Unsicher hier: ist Operator das gleiche wie
                                                                   // Operator Type ?
                nd.setDynamicValue(n.getCalculatedResult()); // here we are saving the Dynamic Values that got
                                                             // calculated in each node after evaluation

                List<InputDTO> inputs = new ArrayList<>();
                if (n.getInputNodes() != null) {
                    for (int i = 0; i < n.getInputNodes().size(); i++) {
                        Node parent = n.getInputNodes().get(i);
                        InputDTO in = new InputDTO();
                        in.setId(parent.getId());
                        in.setOrderIndex(i);
                        inputs.add(in);
                    }
                }
                nd.setInputNodes(inputs);

                // inputTypes / outputTypes / inputLimit are not relevant for the snapshot!
                nodeDTOs.add(nd);
            }
        }

        graphDTO.setNodes(nodeDTOs);
        return graphDTO;
    }

    private static String getNodeType(Node node) {
        if (node == null) {
            return "UNKNOWN";
        }
        if (node instanceof ProviderNode) {
            return "PROVIDER";
        }
        if (node instanceof LogicNode) {
            return "LOGIC";
        }
        if (node instanceof ConstantNode) {
            return "CONSTANT";
        }
        if (node instanceof FinalNode) { // if there is a FinalNode class
            return "FINAL";
        }
        return "UNKNOWN";

    }

    private static Integer getArcId(Node node) {
        if (node instanceof ProviderNode) {
            return ((ProviderNode) node).getArcId();
        }
        return null;
    }

    private static Object getValue(Node node) {
        if (node instanceof ConstantNode) {
            return ((ConstantNode) node).getValue();
        }
        return null;
    }

    private static LogicOperator getOperatorType(Node node) {
        if (node instanceof LogicNode) {
            return ((LogicNode) node).getOperator();
        }
        return null;
    }
}
