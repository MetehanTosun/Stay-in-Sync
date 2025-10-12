package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.dto.vFlow.*;
import de.unistuttgart.graphengine.dto.transformationrule.GraphDTO;
import de.unistuttgart.graphengine.dto.transformationrule.InputDTO;
import de.unistuttgart.graphengine.dto.transformationrule.NodeDTO;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationRuleDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * A specialized mapper in the 'core-management' module.
 * Its purpose is to map the TransformationRule entity into various DTOs
 * required by the API layer for responses.
 */
@ApplicationScoped
public class TransformationRuleMapperService {

    @Inject
    ObjectMapper jsonObjectMapper;

    @Inject
    public TransformationRuleMapperService(ObjectMapper jsonObjectMapper) {
        this.jsonObjectMapper = jsonObjectMapper;
    }

    /**
     * Maps a TransformationRule entity to a lightweight DTO for list views.
     *
     * @param entity The TransformationRule entity from the database.
     * @return The corresponding TransformationRuleDTO without the full graph.
     */
    public TransformationRuleDTO toRuleDTO(TransformationRule entity) {
        Log.debugf("Mapping entity to TransformationRuleDTO with id: %d", entity != null ? entity.id : null);
        if (entity == null) return null;

        Long transformationId = entity.transformation != null ? entity.transformation.id : null;
        Log.infof("Successfully mapped entity id %d to TransformationRuleDTO.", entity.id);
        return new TransformationRuleDTO(
                entity.id,
                entity.name,
                entity.description,
                entity.graphStatus,
                transformationId
        );
    }

    /**
     * Maps a persisted TransformationRule entity to a full GraphDTO.
     *
     * @param entity The TransformationRule entity from the database.
     * @return The corresponding GraphDTO, including id, name, status, and node structure.
     * @throws CoreManagementException if parsing the graph JSON from the entity fails.
     */
    public GraphDTO toGraphDTO(TransformationRule entity) {
        Log.debugf("Mapping entity to GraphDTO with id: %d", entity != null ? entity.id : null);
        if (entity == null || entity.graph == null || entity.graph.graphDefinitionJson == null) {
            return null;
        }

        try {
            // 1. Deserialize the JSON string into the basic GraphDTO structure (containing nodes).
            GraphDTO dto = jsonObjectMapper.readValue(entity.graph.graphDefinitionJson, GraphDTO.class);
            Log.infof("Successfully mapped entity id %d to GraphDTO.", entity.id);
            return dto;

        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to parse graph JSON for TransformationRule with id %d", entity.id);
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR, "Mapping Error", "Failed to create GraphDTO from entity JSON.", e);
        }
    }

    /**
     * Converts the graph from a TransformationRule entity into the VFlowGraphDTO format
     * that the frontend UI expects.
     *
     * @param entity The TransformationRule entity from the database.
     * @return The VFlowGraphDTO with separate lists for nodes and edges.
     * @throws CoreManagementException if parsing the graph JSON from the entity fails.
     */
    public VFlowGraphDTO toVFlowDto(TransformationRule entity) {
        Log.debugf("Mapping entity to VFlowGraphDTO with id: %d", entity != null ? entity.id : null);
        if (entity == null || entity.graph == null || entity.graph.graphDefinitionJson == null) {
            return new VFlowGraphDTO();
        }

        try {
            GraphDTO persistenceDto = jsonObjectMapper.readValue(entity.graph.graphDefinitionJson, GraphDTO.class);

            VFlowGraphDTO vflowDto = new VFlowGraphDTO();

            vflowDto.setNodes(mapNodeDTOsToVFlowNodes(persistenceDto.getNodes()));
            vflowDto.setEdges(createVFlowEdgesFromNodeDTOs(persistenceDto.getNodes()));
            Log.infof("Successfully mapped entity id %d to VFlowGraphDTO.", entity.id);

            return vflowDto;

        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to parse graph JSON for TransformationRule with id %d", entity.id);
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR, "Mapping Error", "Failed to create VFlowGraphDTO from entity JSON.", e);
        }
    }

    /**
     * Helper to map a list of flattened NodeDTOs to a list of VFlowNodeDTOs.
     */
    private List<VFlowNodeDTO> mapNodeDTOsToVFlowNodes(List<NodeDTO> nodeDtos) {
        if (nodeDtos == null) return new ArrayList<>();
        List<VFlowNodeDTO> vflowNodes = new ArrayList<>();
        for (NodeDTO nodeDto : nodeDtos) {
            VFlowNodeDTO vflowNode = new VFlowNodeDTO();
            vflowNode.setId(String.valueOf(nodeDto.getId()));

            PointDTO point = new PointDTO();
            point.setX(nodeDto.getOffsetX());
            point.setY(nodeDto.getOffsetY());
            vflowNode.setPoint(point);

            vflowNode.setType(nodeDto.getNodeType());

            VFlowNodeDataDTO data = new VFlowNodeDataDTO();
            data.setName(nodeDto.getName());
            data.setNodeType(nodeDto.getNodeType());
            data.setArcId(nodeDto.getArcId());
            data.setJsonPath(nodeDto.getJsonPath());
            data.setValue(nodeDto.getValue());
            data.setOperatorType(nodeDto.getOperatorType());
            data.setInputTypes(nodeDto.getInputTypes());
            data.setOutputType(nodeDto.getOutputType());
            data.setInputLimit(nodeDto.getInputLimit());
            data.setChangeDetectionMode(nodeDto.getChangeDetectionMode());
            data.setChangeDetectionActive(nodeDto.isChangeDetectionActive());

            vflowNode.setData(data);
            vflowNodes.add(vflowNode);
        }
        return vflowNodes;
    }

    /**
     * Helper to reconstruct the list of VFlowEdgeDTOs from the flattened inputNodes properties.
     */
    private List<VFlowEdgeDTO> createVFlowEdgesFromNodeDTOs(List<NodeDTO> nodeDtos) {
        if (nodeDtos == null) return new ArrayList<>();

        List<VFlowEdgeDTO> vflowEdges = new ArrayList<>();
        for (NodeDTO targetNodeDto : nodeDtos) {
            if (targetNodeDto.getInputNodes() != null) {
                for (InputDTO inputDto : targetNodeDto.getInputNodes()) {
                    VFlowEdgeDTO edge = new VFlowEdgeDTO();
                    String sourceIdStr = String.valueOf(inputDto.getId());
                    String targetIdStr = String.valueOf(targetNodeDto.getId());

                    edge.setSource(sourceIdStr);
                    edge.setTarget(targetIdStr);
                    edge.setId(sourceIdStr + " -> " + targetIdStr);

                    if(targetNodeDto.getInputTypes() != null && targetNodeDto.getInputTypes().size() > 1) {
                        edge.setTargetHandle("input-" + inputDto.getOrderIndex());
                    }


                    vflowEdges.add(edge);
                }
            }
        }
        return vflowEdges;
    }
}