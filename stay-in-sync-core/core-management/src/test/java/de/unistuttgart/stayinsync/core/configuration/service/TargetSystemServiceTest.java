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

/**
 * Integration tests for {@link de.unistuttgart.stayinsync.core.configuration.service.TargetSystemService}.
 * Verifies CRUD operations for Target Systems, including creation, update, retrieval, and deletion.
 * Tests both successful cases and expected exceptions for invalid or non-existent entities.
 */
@QuarkusTest
class TargetSystemServiceTest {

    @Inject
    TargetSystemService targetSystemService;

    private TargetSystem testTargetSystem;

    /**
     * Prepares a clean test environment before each test execution.
     * Creates a new {@link TargetSystem} entity and persists it to the database.
     */
    @BeforeEach
    @Transactional
    void setUp() {
        testTargetSystem = new TargetSystem();
        testTargetSystem.name = "Test Target System";
        testTargetSystem.apiUrl = "http://test-target.example.com";
        testTargetSystem.description = "Test Target Description";
        testTargetSystem.apiType = "REST";
        testTargetSystem.persist();
    }

    /**
     * Tests successful creation of a new Target System from a DTO.
     * Verifies that all fields are correctly mapped and persisted.
     */
    @Test
    @DisplayName("Should create target system from DTO")
    @Transactional
    void testCreateTargetSystem() {
        TargetSystemDTO dto = new TargetSystemDTO(
                null,
                "NewTargetSystem",
                "https://new-target-api.com",
                "New test target system",
                "GraphQL",
                null,
                null,
                null
        );

        TargetSystemDTO result = targetSystemService.createTargetSystem(dto);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("NewTargetSystem", result.name());
        assertEquals("https://new-target-api.com", result.apiUrl());
        assertEquals("GraphQL", result.apiType());
        assertEquals("New test target system", result.description());
    }

    /**
     * Tests creation of a Target System that includes an OpenAPI specification.
     * Ensures that the OpenAPI JSON is stored and returned correctly.
     */
    @Test
    @DisplayName("Should create target system with OpenAPI spec")
    @Transactional
    void testCreateTargetSystemWithOpenApiSpec() {
        String openApiSpec = "{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"Target API\"}}";
        TargetSystemDTO dto = new TargetSystemDTO(
                null,
                "Target with OpenAPI",
                "https://openapi-target-api.com",
                "Target with OpenAPI spec",
                "REST",
                null,
                openApiSpec,
                null
        );

        TargetSystemDTO result = targetSystemService.createTargetSystem(dto);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Target with OpenAPI", result.name());
        assertEquals(openApiSpec, result.openAPI());
    }

    /**
     * Tests updating an existing Target System.
     * Verifies that the name and description fields are updated correctly.
     */
    @Test
    @DisplayName("Should update target system")
    @Transactional
    void testUpdateTargetSystem() {
        TargetSystemDTO dto = new TargetSystemDTO(
                null,
                "UpdatedTargetSystem",
                "https://updated-target-api.com",
                "Updated description",
                "REST",
                null,
                null,
                null
        );

        TargetSystemDTO result = targetSystemService.updateTargetSystem(testTargetSystem.id, dto);

        assertNotNull(result);
        assertEquals("UpdatedTargetSystem", result.name());
        assertEquals("Updated description", result.description());
    }

    /**
     * Tests behavior when attempting to update a Target System that does not exist.
     * Expects an {@link Exception} to be thrown.
     */
    @Test
    @DisplayName("Should throw exception when updating non-existent target system")
    @Transactional
    void testUpdateNonExistentTargetSystem() {
        TargetSystemDTO dto = new TargetSystemDTO(
                null,
                "UpdatedTargetSystem",
                "https://updated-target-api.com",
                "Updated description",
                "REST",
                null,
                null,
                null
        );

        assertThrows(Exception.class, () -> {
            targetSystemService.updateTargetSystem(9999L, dto);
        });
    }

    /**
     * Tests retrieval of all Target Systems.
     * Verifies that the result list is non-empty and contains the expected entity.
     */
    @Test
    @DisplayName("Should find all target systems")
    @Transactional
    void testFindAllTargetSystems() {
        List<TargetSystemDTO> result = targetSystemService.findAll();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(ts -> ts.name().equals("Test Target System")));
    }

    /**
     * Tests retrieval of a Target System by its ID.
     * Verifies that the correct entity and field values are returned.
     */
    @Test
    @DisplayName("Should find target system by ID")
    @Transactional
    void testFindTargetSystemById() {
        Optional<TargetSystem> result = targetSystemService.findById(testTargetSystem.id);

        assertTrue(result.isPresent());
        assertEquals("Test Target System", result.get().name);
        assertEquals("http://test-target.example.com", result.get().apiUrl);
    }

    /**
     * Tests retrieval of a Target System using a non-existent ID.
     * Verifies that an empty Optional is returned.
     */
    @Test
    @DisplayName("Should return empty optional for non-existent target system")
    @Transactional
    void testFindTargetSystemByIdNotFound() {
        Optional<TargetSystem> result = targetSystemService.findById(9999L);

        assertFalse(result.isPresent());
    }

    /**
     * Tests successful deletion of a Target System by ID.
     * Ensures that the entity is removed from the database and cannot be found afterward.
     */
    @Test
    @DisplayName("Should delete target system by ID")
    @Transactional
    void testDeleteTargetSystem() {
        boolean result = targetSystemService.delete(testTargetSystem.id);

        assertTrue(result);

        Optional<TargetSystem> deleted = targetSystemService.findById(testTargetSystem.id);
        assertFalse(deleted.isPresent());
    }

    /**
     * Tests deletion behavior for a non-existent Target System ID.
     * Verifies that the method returns false without throwing an exception.
     */
    @Test
    @DisplayName("Should return false when deleting non-existent target system")
    @Transactional
    void testDeleteNonExistentTargetSystem() {
        boolean result = targetSystemService.delete(9999L);

        assertFalse(result);
    }
}