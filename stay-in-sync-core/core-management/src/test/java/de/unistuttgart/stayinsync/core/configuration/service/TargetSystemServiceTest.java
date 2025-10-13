package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TargetSystemServiceTest {

    @Inject
    TargetSystemService targetSystemService;

    private TargetSystem testTargetSystem;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing data
        // Create new TargetSystem instead of deleting all to avoid foreign key constraints
        
        // Create a test target system
        testTargetSystem = new TargetSystem();
        testTargetSystem.name = "Test Target System";
        testTargetSystem.apiUrl = "http://test-target.example.com";
        testTargetSystem.description = "Test Target Description";
        testTargetSystem.apiType = "REST";
        testTargetSystem.persist();
    }

    @Test
    @DisplayName("Should create target system from DTO")
    @Transactional
    void testCreateTargetSystem() {
        // Arrange
        TargetSystemDTO dto = new TargetSystemDTO(
                null, // id - don't set ID for creation
                "NewTargetSystem",
                "https://new-target-api.com",
                "New test target system",
                "GraphQL",
                null, // aasId
                null, // openAPI
                null  // targetSystemEndpointIds
        );

        // Act
        TargetSystemDTO result = targetSystemService.createTargetSystem(dto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("NewTargetSystem", result.name());
        assertEquals("https://new-target-api.com", result.apiUrl());
        assertEquals("GraphQL", result.apiType());
        assertEquals("New test target system", result.description());
    }

    @Test
    @DisplayName("Should create target system with OpenAPI spec")
    @Transactional
    void testCreateTargetSystemWithOpenApiSpec() {
        // Arrange
        String openApiSpec = "{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"Target API\"}}";
        TargetSystemDTO dto = new TargetSystemDTO(
                null, // id
                "Target with OpenAPI",
                "https://openapi-target-api.com",
                "Target with OpenAPI spec",
                "REST",
                null, // aasId
                openApiSpec, // openAPI
                null  // targetSystemEndpointIds
        );

        // Act
        TargetSystemDTO result = targetSystemService.createTargetSystem(dto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Target with OpenAPI", result.name());
        assertEquals(openApiSpec, result.openAPI());
    }

    @Test
    @DisplayName("Should update target system")
    @Transactional
    void testUpdateTargetSystem() {
        // Arrange
        TargetSystemDTO dto = new TargetSystemDTO(
                null, // id - don't set ID for update
                "UpdatedTargetSystem",
                "https://updated-target-api.com",
                "Updated description",
                "REST",
                null, // aasId
                null, // openAPI
                null  // targetSystemEndpointIds
        );

        // Act
        TargetSystemDTO result = targetSystemService.updateTargetSystem(testTargetSystem.id, dto);

        // Assert
        assertNotNull(result);
        assertEquals("UpdatedTargetSystem", result.name());
        assertEquals("Updated description", result.description());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent target system")
    @Transactional
    void testUpdateNonExistentTargetSystem() {
        // Arrange
        TargetSystemDTO dto = new TargetSystemDTO(
                null, // id
                "UpdatedTargetSystem",
                "https://updated-target-api.com",
                "Updated description",
                "REST",
                null, // aasId
                null, // openAPI
                null  // targetSystemEndpointIds
        );

        // Act & Assert
        assertThrows(Exception.class, () -> {
            targetSystemService.updateTargetSystem(9999L, dto);
        });
    }

    @Test
    @DisplayName("Should find all target systems")
    @Transactional
    void testFindAllTargetSystems() {
        // Act
        List<TargetSystemDTO> result = targetSystemService.findAll();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(ts -> ts.name().equals("Test Target System")));
    }

    @Test
    @DisplayName("Should find target system by ID")
    @Transactional
    void testFindTargetSystemById() {
        // Act
        Optional<TargetSystem> result = targetSystemService.findById(testTargetSystem.id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Target System", result.get().name);
        assertEquals("http://test-target.example.com", result.get().apiUrl);
    }

    @Test
    @DisplayName("Should return empty optional for non-existent target system")
    @Transactional
    void testFindTargetSystemByIdNotFound() {
        // Act
        Optional<TargetSystem> result = targetSystemService.findById(9999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should delete target system by ID")
    @Transactional
    void testDeleteTargetSystem() {
        // Act
        boolean result = targetSystemService.delete(testTargetSystem.id);

        // Assert
        assertTrue(result);
        
        // Verify the target system is deleted
        Optional<TargetSystem> deleted = targetSystemService.findById(testTargetSystem.id);
        assertFalse(deleted.isPresent());
    }

    @Test
    @DisplayName("Should return false when deleting non-existent target system")
    @Transactional
    void testDeleteNonExistentTargetSystem() {
        // Act
        boolean result = targetSystemService.delete(9999L);

        // Assert
        assertFalse(result);
    }
}