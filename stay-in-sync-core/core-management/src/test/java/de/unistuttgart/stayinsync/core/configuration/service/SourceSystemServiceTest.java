package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
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
class SourceSystemServiceTest {

    @Inject
    SourceSystemService sourceSystemService;

    private SourceSystem testSourceSystem;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create a test source system
        testSourceSystem = new SourceSystem();
        testSourceSystem.name = "Test Source System";
        testSourceSystem.apiUrl = "http://test-source.example.com";
        testSourceSystem.description = "Test Source Description";
        testSourceSystem.apiType = "REST";
        testSourceSystem.persist();
    }

    @Test
    @DisplayName("Should find all source systems")
    @Transactional
    void testFindAllSourceSystems() {
        // Act
        List<SourceSystem> result = sourceSystemService.findAllSourceSystems();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(ss -> ss.name.equals("Test Source System")));
    }

    @Test
    @DisplayName("Should find source system by ID")
    @Transactional
    void testFindSourceSystemById() {
        // Act
        Optional<SourceSystem> result = sourceSystemService.findSourceSystemById(testSourceSystem.id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Source System", result.get().name);
        assertEquals("http://test-source.example.com", result.get().apiUrl);
    }

    @Test
    @DisplayName("Should return empty optional for non-existent source system")
    @Transactional
    void testFindSourceSystemByIdNotFound() {
        // Act
        Optional<SourceSystem> result = sourceSystemService.findSourceSystemById(9999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should create source system from DTO")
    @Transactional
    void testCreateSourceSystem() {
        // Arrange
        CreateSourceSystemDTO createDto = new CreateSourceSystemDTO(
                null, // id
                "New Source System",
                "http://new-source.example.com",
                "New source description",
                "REST",
                null, // aasId
                null, // apiAuthType
                null, // authConfig
                null  // openApiSpec
        );

        // Act
        SourceSystem result = sourceSystemService.createSourceSystem(createDto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.id);
        assertEquals("New Source System", result.name);
        assertEquals("http://new-source.example.com", result.apiUrl);
        assertEquals("New source description", result.description);
        assertEquals("REST", result.apiType);
    }

    @Test
    @DisplayName("Should create source system with OpenAPI spec")
    @Transactional
    void testCreateSourceSystemWithOpenApiSpec() {
        // Arrange
        String openApiSpec = "{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"Test API\"}}";
        CreateSourceSystemDTO createDto = new CreateSourceSystemDTO(
                null, // id
                "Source with OpenAPI",
                "http://openapi-source.example.com",
                "Source with OpenAPI spec",
                "REST",
                null, // aasId
                null, // apiAuthType
                null, // authConfig
                openApiSpec
        );

        // Act
        SourceSystem result = sourceSystemService.createSourceSystem(createDto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.id);
        assertEquals("Source with OpenAPI", result.name);
        assertEquals(openApiSpec, result.openApiSpec);
    }

    @Test
    @DisplayName("Should update source system")
    @Transactional
    void testUpdateSourceSystem() {
        // Arrange
        CreateSourceSystemDTO updateDto = new CreateSourceSystemDTO(
                testSourceSystem.id, // id
                "Updated Source System",
                "http://updated-source.example.com",
                "Updated description",
                "GraphQL",
                null, // aasId
                null, // apiAuthType
                null, // authConfig
                null  // openApiSpec
        );

        // Act
        Optional<SourceSystem> result = sourceSystemService.updateSourceSystem(updateDto);

        // Assert
        assertTrue(result.isPresent());
        SourceSystem updated = result.get();
        assertEquals("Updated Source System", updated.name);
        assertEquals("http://updated-source.example.com", updated.apiUrl);
        assertEquals("Updated description", updated.description);
        assertEquals("GraphQL", updated.apiType);
    }

    @Test
    @DisplayName("Should return empty optional when updating non-existent source system")
    @Transactional
    void testUpdateNonExistentSourceSystem() {
        // Arrange
        CreateSourceSystemDTO updateDto = new CreateSourceSystemDTO(
                9999L, // non-existent id
                "Updated Source System",
                "http://updated-source.example.com",
                "Updated description",
                "GraphQL",
                null, // aasId
                null, // apiAuthType
                null, // authConfig
                null  // openApiSpec
        );

        // Act
        Optional<SourceSystem> result = sourceSystemService.updateSourceSystem(updateDto);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should delete source system by ID")
    @Transactional
    void testDeleteSourceSystem() {
        // Act
        boolean result = sourceSystemService.deleteSourceSystemById(testSourceSystem.id);

        // Assert
        assertTrue(result);
        
        // Verify the source system is deleted
        Optional<SourceSystem> deleted = sourceSystemService.findSourceSystemById(testSourceSystem.id);
        assertFalse(deleted.isPresent());
    }

    @Test
    @DisplayName("Should return false when deleting non-existent source system")
    @Transactional
    void testDeleteNonExistentSourceSystem() {
        // Act
        boolean result = sourceSystemService.deleteSourceSystemById(9999L);

        // Assert
        assertFalse(result);
    }
}