package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
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

/**
 * Integration tests for {@link de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService}.
 * Verifies CRUD operations for Source Systems, including creation, update, retrieval, and deletion.
 * Tests cover both success cases and expected failure scenarios for invalid entities.
 */
@QuarkusTest
class SourceSystemServiceTest {

    @Inject
    SourceSystemService sourceSystemService;

    private SourceSystem testSourceSystem;

    /**
     * Initializes a test {@link SourceSystem} entity before each test.
     * Persists it to the database to ensure consistent state for all CRUD operation tests.
     */
    @BeforeEach
    @Transactional
    void setUp() {
        testSourceSystem = new SourceSystem();
        testSourceSystem.name = "Test Source System";
        testSourceSystem.apiUrl = "http://test-source.example.com";
        testSourceSystem.description = "Test Source Description";
        testSourceSystem.apiType = "REST";
        testSourceSystem.persist();
    }

    /**
     * Tests retrieval of all existing Source Systems.
     * Ensures that the persisted test system is included in the result list.
     */
    @Test
    @DisplayName("Should find all source systems")
    @Transactional
    void testFindAllSourceSystems() {
        List<SourceSystem> result = sourceSystemService.findAllSourceSystems();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(ss -> ss.name.equals("Test Source System")));
    }

    /**
     * Tests successful retrieval of a Source System by its ID.
     * Verifies that all fields match the persisted entity.
     */
    @Test
    @DisplayName("Should find source system by ID")
    @Transactional
    void testFindSourceSystemById() {
        Optional<SourceSystem> result = sourceSystemService.findSourceSystemById(testSourceSystem.id);

        assertTrue(result.isPresent());
        assertEquals("Test Source System", result.get().name);
        assertEquals("http://test-source.example.com", result.get().apiUrl);
    }

    /**
     * Tests retrieval of a non-existent Source System.
     * Verifies that an empty {@link Optional} is returned.
     */
    @Test
    @DisplayName("Should return empty optional for non-existent source system")
    @Transactional
    void testFindSourceSystemByIdNotFound() {
        Optional<SourceSystem> result = sourceSystemService.findSourceSystemById(9999L);

        assertFalse(result.isPresent());
    }

    /**
     * Tests creation of a new Source System from a {@link CreateSourceSystemDTO}.
     * Verifies correct field persistence and automatic ID generation.
     */
    @Test
    @DisplayName("Should create source system from DTO")
    @Transactional
    void testCreateSourceSystem() {
        CreateSourceSystemDTO createDto = new CreateSourceSystemDTO(
                null,
                "New Source System",
                "http://new-source.example.com",
                "New source description",
                "REST",
                null,
                null,
                null,
                null
        );

        SourceSystem result = sourceSystemService.createSourceSystem(createDto);

        assertNotNull(result);
        assertNotNull(result.id);
        assertEquals("New Source System", result.name);
        assertEquals("http://new-source.example.com", result.apiUrl);
        assertEquals("New source description", result.description);
        assertEquals("REST", result.apiType);
    }

    /**
     * Tests creation of a Source System that includes an OpenAPI specification.
     * Verifies that the specification content is stored correctly.
     */
    @Test
    @DisplayName("Should create source system with OpenAPI spec")
    @Transactional
    void testCreateSourceSystemWithOpenApiSpec() {
        String openApiSpec = "{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"Test API\"}}";
        CreateSourceSystemDTO createDto = new CreateSourceSystemDTO(
                null,
                "Source with OpenAPI",
                "http://openapi-source.example.com",
                "Source with OpenAPI spec",
                "REST",
                null,
                null,
                null,
                openApiSpec
        );

        SourceSystem result = sourceSystemService.createSourceSystem(createDto);

        assertNotNull(result);
        assertNotNull(result.id);
        assertEquals("Source with OpenAPI", result.name);
        assertEquals(openApiSpec, result.openApiSpec);
    }

    /**
     * Tests updating an existing Source System using a {@link CreateSourceSystemDTO}.
     * Ensures all fields are updated and persisted correctly.
     */
    @Test
    @DisplayName("Should update source system")
    @Transactional
    void testUpdateSourceSystem() {
        CreateSourceSystemDTO updateDto = new CreateSourceSystemDTO(
                testSourceSystem.id,
                "Updated Source System",
                "http://updated-source.example.com",
                "Updated description",
                "GraphQL",
                null,
                null,
                null,
                null
        );

        Optional<SourceSystem> result = sourceSystemService.updateSourceSystem(updateDto);

        assertTrue(result.isPresent());
        SourceSystem updated = result.get();
        assertEquals("Updated Source System", updated.name);
        assertEquals("http://updated-source.example.com", updated.apiUrl);
        assertEquals("Updated description", updated.description);
        assertEquals("GraphQL", updated.apiType);
    }

    /**
     * Tests behavior when updating a non-existent Source System.
     * Verifies that the returned {@link Optional} is empty.
     */
    @Test
    @DisplayName("Should return empty optional when updating non-existent source system")
    @Transactional
    void testUpdateNonExistentSourceSystem() {
        CreateSourceSystemDTO updateDto = new CreateSourceSystemDTO(
                9999L,
                "Updated Source System",
                "http://updated-source.example.com",
                "Updated description",
                "GraphQL",
                null,
                null,
                null,
                null
        );

        Optional<SourceSystem> result = sourceSystemService.updateSourceSystem(updateDto);

        assertFalse(result.isPresent());
    }

    /**
     * Tests deletion of an existing Source System by its ID.
     * Verifies that the system is removed from the database and cannot be retrieved afterward.
     */
    @Test
    @DisplayName("Should delete source system by ID")
    @Transactional
    void testDeleteSourceSystem() {
        boolean result = sourceSystemService.deleteSourceSystemById(testSourceSystem.id);

        assertTrue(result);
        
        Optional<SourceSystem> deleted = sourceSystemService.findSourceSystemById(testSourceSystem.id);
        assertFalse(deleted.isPresent());
    }

    /**
     * Tests deletion behavior for a non-existent Source System ID.
     * Verifies that the method returns false without throwing an exception.
     */
    @Test
    @DisplayName("Should return false when deleting non-existent source system")
    @Transactional
    void testDeleteNonExistentSourceSystem() {
        boolean result = sourceSystemService.deleteSourceSystemById(9999L);

        assertFalse(result);
    }
}