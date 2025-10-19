package de.unistuttgart.stayinsync.core.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.dto.transformationrule.GraphDTO;
import de.unistuttgart.graphengine.dto.transformationrule.TransformationRulePayloadDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowGraphDTO;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.GraphStorageService;
import de.unistuttgart.graphengine.service.GraphValidatorService;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.TransformationRuleService;
import de.unistuttgart.graphengine.validation_error.GraphStatus;
import de.unistuttgart.graphengine.validation_error.ValidationError;
import de.unistuttgart.graphengine.service.GraphMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the TransformationRuleService.
 * These tests focus on business logic and service interactions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransformationRuleService Unit Tests")
class TransformationRuleServiceTest {

    @InjectMocks
    private TransformationRuleService ruleService;

    @Mock
    private GraphMapper mapper;
    @Mock
    private GraphValidatorService validator;
    @Mock
    private GraphStorageService storageService;
    @Mock
    private ObjectMapper jsonObjectMapper;

    // ========== Create Rule Tests ==========

    @Test
    @DisplayName("Should create rule with default graph successfully")
    void shouldCreateRuleWithDefaultGraph() throws JsonProcessingException {
        // Arrange
        TransformationRulePayloadDTO payload = createPayload("New Rule", "Description");
        when(storageService.findRuleByName("New Rule")).thenReturn(Optional.empty());
        doReturn("{\"nodes\":[]}").when(jsonObjectMapper).writeValueAsString(any(GraphDTO.class));

        // Act
        GraphStorageService.PersistenceResult result = ruleService.createRule(payload);

        // Assert
        ArgumentCaptor<TransformationRule> ruleCaptor = ArgumentCaptor.forClass(TransformationRule.class);
        verify(storageService).persistRule(ruleCaptor.capture());

        TransformationRule createdRule = ruleCaptor.getValue();
        assertEquals("New Rule", createdRule.name);
        assertEquals("Description", createdRule.description);
        assertEquals(GraphStatus.FINALIZED, createdRule.graphStatus);
        assertNotNull(createdRule.graph);
        assertTrue(result.validationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when rule name already exists")
    void shouldThrowExceptionForDuplicateName() {
        // Arrange
        TransformationRulePayloadDTO payload = createPayload("Existing Rule", "Description");
        TransformationRule existingRule = new TransformationRule();
        when(storageService.findRuleByName("Existing Rule")).thenReturn(Optional.of(existingRule));

        // Act & Assert
        CoreManagementException exception = assertThrows(CoreManagementException.class, () -> {
            ruleService.createRule(payload);
        });

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("already exists"));
        verify(storageService, never()).persistRule(any());
    }

    @Test
    @DisplayName("Should throw exception when JSON serialization fails during creation")
    void shouldThrowExceptionWhenSerializationFailsDuringCreation() throws JsonProcessingException {
        // Arrange
        TransformationRulePayloadDTO payload = createPayload("New Rule", "Description");
        when(storageService.findRuleByName("New Rule")).thenReturn(Optional.empty());
        doThrow(new JsonProcessingException("Serialization failed") {}).when(jsonObjectMapper).writeValueAsString(any(GraphDTO.class));

        // Act & Assert
        CoreManagementException exception = assertThrows(CoreManagementException.class, () -> {
            ruleService.createRule(payload);
        });

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("create default graph"));
        verify(storageService, never()).persistRule(any());
    }

    // ========== Update Rule Metadata Tests ==========

    @Test
    @DisplayName("Should update rule metadata successfully")
    void shouldUpdateRuleMetadataSuccessfully() {
        // Arrange
        Long ruleId = 1L;
        TransformationRule existingRule = createTestRule();
        TransformationRulePayloadDTO updatePayload = createPayload("Updated Name", "Updated Description");

        when(storageService.findRuleById(ruleId)).thenReturn(existingRule);

        // Act
        TransformationRule result = ruleService.updateRuleMetadata(ruleId, updatePayload);

        // Assert
        assertEquals("Updated Name", result.name);
        assertEquals("Updated Description", result.description);
    }

    @Test
    @DisplayName("Should throw exception when rule not found for metadata update")
    void shouldThrowExceptionWhenRuleNotFoundForMetadataUpdate() {
        // Arrange
        Long nonExistentId = 999L;
        TransformationRulePayloadDTO payload = createPayload("Name", "Description");
        when(storageService.findRuleById(nonExistentId)).thenReturn(null);

        // Act & Assert
        CoreManagementException exception = assertThrows(CoreManagementException.class, () -> {
            ruleService.updateRuleMetadata(nonExistentId, payload);
        });

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("not found") ||
                exception.getMessage().contains("Not Found"));
    }

    // ========== Update Rule Graph Tests ==========

    @Test
    @DisplayName("Should update graph to finalized status when validation passes")
    void shouldUpdateGraphToFinalizedWhenValidationPasses() throws JsonProcessingException {
        // Arrange
        Long ruleId = 1L;
        VFlowGraphDTO vflowGraph = new VFlowGraphDTO();
        TransformationRule existingRule = createTestRule();

        setupValidGraphMocks();
        when(storageService.findRuleById(ruleId)).thenReturn(existingRule);

        // Act
        GraphStorageService.PersistenceResult result = ruleService.updateRuleGraph(ruleId, vflowGraph);

        // Assert
        assertEquals(GraphStatus.FINALIZED, result.entity().graphStatus);
        assertTrue(result.validationErrors().isEmpty());
        assertNull(result.entity().validationErrorsJson);

        verify(mapper).vflowToGraphDto(vflowGraph);
        verify(mapper).toNodeGraph(any(GraphDTO.class));
        verify(validator).validateGraph(anyList(), anyInt());
        verify(jsonObjectMapper).writeValueAsString(any(GraphDTO.class));
    }

    @Test
    @DisplayName("Should update graph to draft status when validation fails")
    void shouldUpdateGraphToDraftWhenValidationFails() throws JsonProcessingException {
        // Arrange
        Long ruleId = 1L;
        VFlowGraphDTO vflowGraph = new VFlowGraphDTO();
        TransformationRule existingRule = createTestRule();
        List<ValidationError> validationErrors = List.of(mock(ValidationError.class));

        setupInvalidGraphMocks(validationErrors);
        when(storageService.findRuleById(ruleId)).thenReturn(existingRule);

        // Act
        GraphStorageService.PersistenceResult result = ruleService.updateRuleGraph(ruleId, vflowGraph);

        // Assert
        assertEquals(GraphStatus.DRAFT, result.entity().graphStatus);
        assertEquals(1, result.validationErrors().size());
        assertNotNull(result.entity().validationErrorsJson);

        verify(jsonObjectMapper, times(2)).writeValueAsString(any()); // Graph + Errors
    }

    @Test
    @DisplayName("Should throw exception when rule not found for graph update")
    void shouldThrowExceptionWhenRuleNotFoundForGraphUpdate() {
        // Arrange
        Long nonExistentId = 999L;
        VFlowGraphDTO vflowGraph = new VFlowGraphDTO();
        when(storageService.findRuleById(nonExistentId)).thenReturn(null);

        // Act & Assert
        CoreManagementException exception = assertThrows(CoreManagementException.class, () -> {
            ruleService.updateRuleGraph(nonExistentId, vflowGraph);
        });

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("not found") ||
                exception.getMessage().contains("Not Found"));
    }

    @Test
    @DisplayName("Should throw exception when JSON serialization fails during graph update")
    void shouldThrowExceptionWhenSerializationFailsDuringGraphUpdate() throws JsonProcessingException {
        // Arrange
        Long ruleId = 1L;
        VFlowGraphDTO vflowGraph = new VFlowGraphDTO();
        TransformationRule existingRule = createTestRule();

        when(storageService.findRuleById(ruleId)).thenReturn(existingRule);
        when(mapper.vflowToGraphDto(any())).thenReturn(new GraphDTO());
        when(mapper.toNodeGraph(any())).thenReturn(
                new GraphMapper.MappingResult(List.of(mock(Node.class)), Collections.emptyList())
        );
        when(validator.validateGraph(any(), anyInt())).thenReturn(Collections.emptyList());
        doThrow(new JsonProcessingException("Serialization failed") {}).when(jsonObjectMapper).writeValueAsString(any(GraphDTO.class));

        // Act & Assert
        CoreManagementException exception = assertThrows(CoreManagementException.class, () -> {
            ruleService.updateRuleGraph(ruleId, vflowGraph);
        });

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("serialize") ||
                exception.getMessage().contains("Serialization"));
    }

    // ========== Get Validation Errors Tests ==========

    @Test
    @DisplayName("Should return validation errors for draft rule")
    void shouldReturnValidationErrorsForDraftRule() throws JsonProcessingException {
        // Arrange
        Long ruleId = 1L;
        TransformationRule draftRule = createTestRule();
        draftRule.graphStatus = GraphStatus.DRAFT;
        draftRule.validationErrorsJson = "[{\"message\":\"error\"}]";

        List<ValidationError> expectedErrors = List.of(mock(ValidationError.class));

        when(storageService.findRuleById(ruleId)).thenReturn(draftRule);
        doReturn(expectedErrors).when(jsonObjectMapper).readValue(eq(draftRule.validationErrorsJson), any(com.fasterxml.jackson.core.type.TypeReference.class));

        // Act
        List<ValidationError> result = ruleService.getValidationErrorsForRule(ruleId);

        // Assert
        assertEquals(expectedErrors, result);
    }

    @Test
    @DisplayName("Should return empty list for finalized rule")
    void shouldReturnEmptyListForFinalizedRule() {
        // Arrange
        Long ruleId = 1L;
        TransformationRule finalizedRule = createTestRule();
        finalizedRule.graphStatus = GraphStatus.FINALIZED;

        when(storageService.findRuleById(ruleId)).thenReturn(finalizedRule);

        // Act
        List<ValidationError> result = ruleService.getValidationErrorsForRule(ruleId);

        // Assert
        assertTrue(result.isEmpty(), "Finalized rule should return empty validation errors list");
    }

    // ========== Helper Methods ==========

    private TransformationRulePayloadDTO createPayload(String name, String description) {
        TransformationRulePayloadDTO payload = new TransformationRulePayloadDTO();
        payload.setName(name);
        payload.setDescription(description);
        return payload;
    }

    private TransformationRule createTestRule() {
        TransformationRule rule = new TransformationRule();
        rule.name = "Test Rule";
        rule.description = "Test Description";
        rule.graph = new LogicGraphEntity();
        rule.graphStatus = GraphStatus.DRAFT;
        return rule;
    }

    private void setupValidGraphMocks() throws JsonProcessingException {
        when(mapper.vflowToGraphDto(any())).thenReturn(new GraphDTO());
        when(mapper.toNodeGraph(any())).thenReturn(
                new GraphMapper.MappingResult(List.of(mock(Node.class)), Collections.emptyList())
        );
        when(validator.validateGraph(any(), anyInt())).thenReturn(Collections.emptyList());
        doReturn("{}").when(jsonObjectMapper).writeValueAsString(any(GraphDTO.class));
    }

    private void setupInvalidGraphMocks(List<ValidationError> validationErrors) throws JsonProcessingException {
        when(mapper.vflowToGraphDto(any())).thenReturn(new GraphDTO());
        when(mapper.toNodeGraph(any())).thenReturn(
                new GraphMapper.MappingResult(List.of(mock(Node.class)), Collections.emptyList())
        );
        when(validator.validateGraph(any(), anyInt())).thenReturn(validationErrors);
        doReturn("{}").when(jsonObjectMapper).writeValueAsString(any(GraphDTO.class));
        doReturn("[{\"error\":\"validation failed\"}]").when(jsonObjectMapper).writeValueAsString(eq(validationErrors));
    }
}
