package de.unistuttgart.stayinsync.core.service.transformationrule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.dto.transformationrule.GraphDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowGraphDTO;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationRuleDTO;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.TransformationRuleMapperService;
import de.unistuttgart.graphengine.validation_error.GraphStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the TransformationRuleMapperService.
 * This test verifies the mapping logic from the domain entity to different DTOs.
 */
public class TransformationRuleMapperServiceTest {

    private TransformationRuleMapperService mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Da der Mapper jetzt JSON parsen muss, injizieren wir die Abhängigkeit manuell.
        mapper = new TransformationRuleMapperService(objectMapper);
    }

    @Test
    void testToRuleDTO_ShouldMapAllFieldsCorrectly() {
        // ARRANGE
        TransformationRule entity = new TransformationRule();
        entity.id = 1L;
        entity.name = "Test Rule";
        entity.description = "A test description";
        entity.graphStatus = GraphStatus.DRAFT;

        // ACT
        TransformationRuleDTO dto = mapper.toRuleDTO(entity);

        // ASSERT
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test Rule", dto.getName());
        assertEquals("A test description", dto.getDescription());
        assertEquals(GraphStatus.DRAFT, dto.getGraphStatus());
    }

    // ### NEUER TESTFALL ###
    @Test
    void testToGraphDTO_WithValidJson_ShouldReturnGraphDTO() {
        // ARRANGE
        TransformationRule entity = new TransformationRule();
        LogicGraphEntity graph = new LogicGraphEntity();
        graph.graphDefinitionJson = "{\"nodes\":[{\"id\":0}]}";
        entity.graph = graph;

        // ACT
        GraphDTO graphDto = mapper.toGraphDTO(entity);

        // ASSERT
        assertNotNull(graphDto);
        assertEquals(1, graphDto.getNodes().size());
    }

    @Test
    void testToVFlowDto_WithValidJson_ShouldMapNodesAndEdges() {
        // ARRANGE
        TransformationRule entity = new TransformationRule();
        LogicGraphEntity graph = new LogicGraphEntity();
        graph.graphDefinitionJson = "{\"nodes\":[" +
                "{\"id\":0,\"nodeType\":\"CONFIG\"}," +
                "{\"id\":1,\"nodeType\":\"FINAL\",\"inputNodes\":[{\"id\":0,\"orderIndex\":0}]}" +
                "]}";
        entity.graph = graph;

        // ACT
        VFlowGraphDTO vflowDto = mapper.toVFlowDto(entity);

        // ASSERT
        assertNotNull(vflowDto);
        assertEquals(2, vflowDto.getNodes().size(), "Should have mapped 2 nodes.");
        assertEquals(1, vflowDto.getEdges().size(), "Should have created 1 edge.");
        assertEquals("0", vflowDto.getEdges().get(0).getSource());
        assertEquals("1", vflowDto.getEdges().get(0).getTarget());
    }

    // ### NEUER TESTFALL ###
    @Test
    void testMapping_WithMalformedJson_ShouldThrowException() {
        // ARRANGE
        TransformationRule entity = new TransformationRule();
        LogicGraphEntity graph = new LogicGraphEntity();
        graph.graphDefinitionJson = "{\"nodes\":[{\"id\":0,}}"; // Kaputtes JSON mit extra Komma
        entity.graph = graph;

        // ACT & ASSERT
        // Prüfe, ob toGraphDTO eine Exception wirft
        assertThrows(CoreManagementException.class, () -> {
            mapper.toGraphDTO(entity);
        }, "toGraphDTO should throw CoreManagementException for malformed JSON.");

        // Prüfe, ob toVFlowDto eine Exception wirft
        assertThrows(CoreManagementException.class, () -> {
            mapper.toVFlowDto(entity);
        }, "toVFlowDto should throw CoreManagementException for malformed JSON.");
    }

    // ### NEUER TESTFALL ###
    @Test
    void testMapping_WithNullGraph_ShouldReturnEmptyOrNull() {
        // ARRANGE
        TransformationRule entityWithNullGraph = new TransformationRule();
        entityWithNullGraph.graph = null;

        // ACT & ASSERT
        assertNull(mapper.toGraphDTO(entityWithNullGraph), "toGraphDTO should return null for a null graph entity.");

        VFlowGraphDTO vflowDto = mapper.toVFlowDto(entityWithNullGraph);
        assertNotNull(vflowDto, "toVFlowDto should return an empty object, not null.");
        assertTrue(vflowDto.getNodes().isEmpty() && vflowDto.getEdges().isEmpty(), "toVFlowDto should be empty for a null graph entity.");
    }

    @Test
    void testToRuleDTO_WithNullEntity_ShouldReturnNull() {
        // ARRANGE
        TransformationRule entity = null;

        // ACT
        TransformationRuleDTO dto = mapper.toRuleDTO(entity);

        // ASSERT
        assertNull(dto, "Mapping a null entity should result in a null DTO.");
    }
}