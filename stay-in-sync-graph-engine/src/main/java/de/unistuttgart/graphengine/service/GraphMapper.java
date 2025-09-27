package de.unistuttgart.graphengine.service;

import de.unistuttgart.graphengine.dto.vFlow.VFlowEdgeDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowGraphDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowNodeDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowNodeDataDTO;
import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import de.unistuttgart.graphengine.logic_operator.LogicOperator;
import de.unistuttgart.graphengine.nodes.*;
import de.unistuttgart.graphengine.dto.transformationrule.GraphDTO;
import de.unistuttgart.graphengine.dto.transformationrule.InputDTO;
import de.unistuttgart.graphengine.dto.transformationrule.NodeDTO;
import de.unistuttgart.graphengine.validation_error.NodeConfigurationError;
import de.unistuttgart.graphengine.validation_error.ValidationError;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;

import java.util.*;

/**
 * A central service for mapping between different graph representations:
 * - VFlowGraphDTO (from the frontend)
 * - List<Node> (the internal, in-memory domain model)
 * - GraphDTO (the flattened format for database persistence)
 */
@ApplicationScoped
public class GraphMapper {

    /**
     * A record to hold the result of the mapping process from DTO to Node objects.
     * @param nodes The list of successfully created nodes.
     * @param mappingErrors A list of configuration errors found during mapping.
     */
    public record MappingResult(List<Node> nodes, List<ValidationError> mappingErrors) {}

    // ==========================================================================================
    // SECTION 1: Mapping from Frontend (VFlow) to Persistence Format (GraphDTO)
    // ==========================================================================================

    /**
     * Converts a VFlowGraphDTO from the frontend into the flattened GraphDTO format
     * that is used for persistence in the database.
     *
     * @param vflowDto The graph data from the ngx-vflow UI.
     * @return The flattened GraphDTO ready for persistence.
     */
    public GraphDTO vflowToGraphDto(VFlowGraphDTO vflowDto) {
        Log.debug("Starting mapping from VFlowGraphDTO to GraphDTO.");
        if (vflowDto == null || vflowDto.getNodes() == null || vflowDto.getNodes().isEmpty()) {
            Log.debug("Input VFlowGraphDTO is null or empty, returning new GraphDTO.");
            return new GraphDTO();
        }
        Log.debugf("Mapping VFlowGraphDTO with %d nodes and %d edges.", vflowDto.getNodes().size(), vflowDto.getEdges() != null ? vflowDto.getEdges().size() : 0);

        GraphDTO graphDto = new GraphDTO();

        // Pass 1: Create all flat NodeDTOs from the vflow nodes.
        Map<String, NodeDTO> nodeDtoMap = mapVFlowNodesToNodeDTOs(vflowDto.getNodes());

        // Pass 2: Apply the edge information to create the `inputNodes` connections.
        if (vflowDto.getEdges() != null) {
            applyVFlowEdgesToInputNodes(nodeDtoMap, vflowDto.getEdges());
        }

        graphDto.setNodes(new ArrayList<>(nodeDtoMap.values()));
        Log.infof("Successfully mapped VFlowGraphDTO to GraphDTO with %d nodes.", graphDto.getNodes().size());
        return graphDto;
    }

    /**
     * Helper to create a map of flattened NodeDTOs from the VFlowNodeDTO list.
     */
    private Map<String, NodeDTO> mapVFlowNodesToNodeDTOs(List<VFlowNodeDTO> vflowNodes) {
        Log.debugf("Creating flat NodeDTOs from %d VFlowNodeDTOs.", vflowNodes.size());
        Map<String, NodeDTO> nodeDtoMap = new HashMap<>();
        for (VFlowNodeDTO vflowNode : vflowNodes) {
            NodeDTO nodeDto = new NodeDTO();
            VFlowNodeDataDTO data = vflowNode.getData();

            nodeDto.setId(Integer.parseInt(vflowNode.getId()));
            nodeDto.setName(data.getName());
            nodeDto.setNodeType(data.getNodeType());
            nodeDto.setOffsetX(vflowNode.getPoint().getX());
            nodeDto.setOffsetY(vflowNode.getPoint().getY());
            nodeDto.setArcId(data.getArcId());
            nodeDto.setJsonPath(data.getJsonPath());
            nodeDto.setValue(data.getValue());
            nodeDto.setOperatorType(data.getOperatorType());
            nodeDto.setInputTypes(data.getInputTypes());
            nodeDto.setOutputType(data.getOutputType());
            nodeDto.setInputLimit(data.getInputLimit());
            nodeDto.setInputNodes(new ArrayList<>()); // Initialize empty list for connections
            nodeDto.setChangeDetectionMode(data.getChangeDetectionMode());
            nodeDto.setChangeDetectionActive(data.isChangeDetectionActive());

            nodeDtoMap.put(vflowNode.getId(), nodeDto);
        }
        Log.debugf("Created %d NodeDTOs.", nodeDtoMap.size());
        return nodeDtoMap;
    }

    /**
     * Helper that iterates through the edges and adds the corresponding
     * InputDTOs to the target NodeDTOs' `inputNodes` list.
     */
    private void applyVFlowEdgesToInputNodes(Map<String, NodeDTO> nodeDtoMap, List<VFlowEdgeDTO> vflowEdges) {
        Log.debugf("Applying %d VFlow edges to connect NodeDTOs.", vflowEdges.size());
        int appliedEdges = 0;
        for (VFlowEdgeDTO edge : vflowEdges) {
            NodeDTO targetNodeDto = nodeDtoMap.get(edge.getTarget());
            if (targetNodeDto == null) {
                Log.warnf("Edge references a non-existent target node with ID: %s", edge.getTarget());
                continue;
            }

            InputDTO inputDto = new InputDTO();
            inputDto.setId(Integer.parseInt(edge.getSource()));
            inputDto.setOrderIndex(parseOrderIndexFromTargetHandle(edge.getTargetHandle()));

            targetNodeDto.getInputNodes().add(inputDto);
            appliedEdges++;
        }
        Log.debugf("Successfully applied %d edges.", appliedEdges);
    }

    // ==========================================================================================
    // SECTION 3: Mapping from Persistence Format (GraphDTO) to Internal Domain Model (List<Node>)
    // ==========================================================================================

    /**
     * Maps a GraphDTO (deserialized from JSON) into a fully connected
     * in-memory graph of Node objects for validation and evaluation.
     */
    public MappingResult toNodeGraph(GraphDTO graphDto)  {
        Log.debug("Starting mapping from GraphDTO to internal Node graph.");
        if (graphDto == null || graphDto.getNodes() == null || graphDto.getNodes().isEmpty()) {
            Log.debug("Input GraphDTO is null or empty, returning empty list.");
            return new MappingResult(new ArrayList<>(), new ArrayList<>());
        }
        Log.debugf("Mapping GraphDTO with %d nodes to internal domain model.", graphDto.getNodes().size());
        Map<Integer, Node> createdNodes = new HashMap<>();
        List<ValidationError> mappingErrors = new ArrayList<>();

        // Pass 1: Create all node instances.
        Log.debug("Pass 1: Creating all Node instances from DTOs.");
            for (NodeDTO dto : graphDto.getNodes()) {
                try {
                    Node node;
                    switch (dto.getNodeType()) {
                        case "PROVIDER":
                            node = new ProviderNode(dto.getJsonPath());
                            ((ProviderNode) node).setArcId(dto.getArcId());
                            break;
                        case "CONSTANT":
                            node = new ConstantNode(dto.getName(), dto.getValue());
                            break;
                        case "LOGIC":
                            node = new LogicNode(dto.getName(), LogicOperator.valueOf(dto.getOperatorType()));
                            break;
                        case "FINAL":
                            node = new FinalNode();
                            break;
                        case "CONFIG":
                            ConfigNode configNode = new ConfigNode();
                            configNode.setActive(dto.isChangeDetectionActive());
                            if (dto.getChangeDetectionMode() != null) {
                                configNode.setMode(ConfigNode.ChangeDetectionMode.valueOf(dto.getChangeDetectionMode()));
                            }
                            configNode.setTimeWindowEnabled(dto.isTimeWindowEnabled());
                            configNode.setTimeWindowMillis(dto.getTimeWindowMillis());
                            node = configNode;
                            break;
                        default:
                            throw new NodeConfigurationException("Unknown nodeType: " + dto.getNodeType());
                    }
                    node.setId(dto.getId());
                    node.setName(dto.getName());
                    node.setOffsetX(dto.getOffsetX());
                    node.setOffsetY(dto.getOffsetY());
                    createdNodes.put(node.getId(), node);
                }

                catch (NodeConfigurationException e) {
                    Log.warnf(e, "A node with invalid configuration was found (ID: %d). It will be skipped.", dto.getId());
                        mappingErrors.add(new NodeConfigurationError(dto.getId(), dto.getName(), e.getMessage()));
                    }
            }

        Log.debugf("Created %d Node instances.", createdNodes.size());

        // Pass 2: Apply input connections from the inputNodes property of each NodeDTO.
        Log.debug("Pass 2: Connecting Node instances based on InputDTOs.");
        for (NodeDTO dto : graphDto.getNodes()) {
            Node targetNode = createdNodes.get(dto.getId());
            if (dto.getInputNodes() != null && !dto.getInputNodes().isEmpty()) {
                // Ensure the input list is sorted by orderIndex
                dto.getInputNodes().sort(Comparator.comparingInt(InputDTO::getOrderIndex));

                List<Node> orderedInputs = new ArrayList<>();
                for (InputDTO inputDto : dto.getInputNodes()) {
                    Node sourceNode = createdNodes.get(inputDto.getId());
                    if (sourceNode != null) {
                        orderedInputs.add(sourceNode);
                    } else {
                        Log.warnf("Could not find source node with id %d for target node %d. Connection skipped.", inputDto.getId(), targetNode.getId());
                    }
                }
                targetNode.setInputNodes(orderedInputs);
            }
        }
        Log.debug("Finished connecting nodes.");

        Log.infof("Successfully mapped GraphDTO to an internal graph with %d nodes.", createdNodes.size());
        return new MappingResult(new ArrayList<>(createdNodes.values()), mappingErrors);
    }

    /**
     * Extracts the numeric order index from a vflow target handle string.
     */
    private int parseOrderIndexFromTargetHandle(String targetHandle) {
        if (targetHandle != null && targetHandle.contains("-")) {
            try {
                return Integer.parseInt(targetHandle.split("-")[1]);
            } catch (Exception e) {
                Log.warnf(e, "Could not parse orderIndex from targetHandle: '%s'. Defaulting to 0.", targetHandle);
            }
        }
        return 0;
    }
}