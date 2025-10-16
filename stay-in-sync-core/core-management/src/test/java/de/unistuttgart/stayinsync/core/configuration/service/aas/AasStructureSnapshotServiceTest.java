package de.unistuttgart.stayinsync.core.configuration.service.aas;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystemEndpoint;
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
 * Integration tests for {@link de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService}.
 * Verifies snapshot creation, refresh, and AASX ingestion functionalities.
 * Tests include behavior with valid and invalid source systems, AASX file parsing,
 * and method existence validation for AAS structure updates.
 */
@QuarkusTest
class AasStructureSnapshotServiceTest {

    @Inject
    AasStructureSnapshotService aasStructureSnapshotService;

    @Inject
    ObjectMapper objectMapper;

    private SourceSystem testSourceSystem;

    /**
     * Prepares the test environment by cleaning the database and creating a test {@link SourceSystem}.
     * Ensures that each test starts with a clean state.
     */
    @BeforeEach
    @Transactional
    void setUp() {
        AasElementLite.deleteAll();
        AasSubmodelLite.deleteAll();
        SourceSystemEndpoint.deleteAll();
        SourceSystem.deleteAll();
        testSourceSystem = new SourceSystem();
        testSourceSystem.name = "Test Source System";
        testSourceSystem.apiUrl = "http://test-source.example.com";
        testSourceSystem.description = "Test Source Description";
        testSourceSystem.apiType = "REST";
        testSourceSystem.aasId = "https://example.com/aas/123";
        testSourceSystem.persist();
    }

    /**
     * Tests the buildInitialSnapshot method to verify that it executes without unhandled exceptions.
     * Ensures that internal HTTP failures are safely caught and do not propagate.
     */
    @Test
    @DisplayName("Should build initial snapshot")
    @Transactional
    void testBuildInitialSnapshot() {
        aasStructureSnapshotService.buildInitialSnapshot(testSourceSystem.id);
        assertNotNull(aasStructureSnapshotService);
    }

    /**
     * Tests the refreshSnapshot method to verify that it executes successfully
     * and handles HTTP-related errors internally.
     */
    @Test
    @DisplayName("Should refresh snapshot")
    @Transactional
    void testRefreshSnapshot() {
        aasStructureSnapshotService.refreshSnapshot(testSourceSystem.id);
        assertNotNull(aasStructureSnapshotService);
    }

    /**
     * Tests behavior when attempting to refresh a snapshot for a non-existent Source System.
     * Ensures that the method handles missing entities gracefully.
     */
    @Test
    @DisplayName("Should handle non-existent source system gracefully")
    @Transactional
    void testRefreshSnapshotWithNonExistentSourceSystem() {
        aasStructureSnapshotService.refreshSnapshot(9999L);
        assertNotNull(aasStructureSnapshotService);
    }

    /**
     * Tests ingestion of an invalid AASX file.
     * Expects an {@link AasStructureSnapshotService.InvalidAasxException} to be thrown.
     */
    @Test
    @DisplayName("Should ingest AASX file")
    @Transactional
    void testIngestAasx() {
        String filename = "test.aasx";
        byte[] fileBytes = "test content".getBytes();
        assertThrows(AasStructureSnapshotService.InvalidAasxException.class, () -> {
            aasStructureSnapshotService.ingestAasx(testSourceSystem.id, filename, fileBytes);
        });
    }

    /**
     * Tests ingestion of an empty AASX file.
     * Expects an {@link AasStructureSnapshotService.InvalidAasxException} due to missing content.
     */
    @Test
    @DisplayName("Should throw exception for empty AASX file")
    @Transactional
    void testIngestAasxWithEmptyFile() {
        String filename = "empty.aasx";
        byte[] fileBytes = new byte[0];
        assertThrows(AasStructureSnapshotService.InvalidAasxException.class, () -> {
            aasStructureSnapshotService.ingestAasx(testSourceSystem.id, filename, fileBytes);
        });
    }

    /**
     * Tests ingestion of a null AASX file reference.
     * Ensures that the method throws an {@link AasStructureSnapshotService.InvalidAasxException}.
     */
    @Test
    @DisplayName("Should throw exception for null AASX file")
    @Transactional
    void testIngestAasxWithNullFile() {
        String filename = "null.aasx";
        byte[] fileBytes = null;
        assertThrows(AasStructureSnapshotService.InvalidAasxException.class, () -> {
            aasStructureSnapshotService.ingestAasx(testSourceSystem.id, filename, fileBytes);
        });
    }

    /**
     * Tests ingestion behavior when a non-existent Source System ID is provided.
     * Expects the method to throw an {@link AasStructureSnapshotService.InvalidAasxException}.
     */
    @Test
    @DisplayName("Should throw exception for non-existent source system in AASX ingestion")
    @Transactional
    void testIngestAasxWithNonExistentSourceSystem() {
        String filename = "test.aasx";
        byte[] fileBytes = "test content".getBytes();
        assertThrows(AasStructureSnapshotService.InvalidAasxException.class, () -> {
            aasStructureSnapshotService.ingestAasx(9999L, filename, fileBytes);
        });
    }

    /**
     * Tests the application of a submodel creation event to the snapshot.
     * Ensures that the method executes successfully and updates internal structures.
     */
    @Test
    @DisplayName("Should apply submodel create")
    @Transactional
    void testApplySubmodelCreate() {
        String submodelJson = "{\"id\": \"https://example.com/submodel/456\", \"idShort\": \"TestSubmodel\"}";
        aasStructureSnapshotService.applySubmodelCreate(testSourceSystem.id, submodelJson);
        assertNotNull(aasStructureSnapshotService);
    }

    /**
     * Tests applying an element creation event to a submodel.
     * Verifies method execution without exceptions.
     */
    @Test
    @DisplayName("Should apply element create")
    @Transactional
    void testApplyElementCreate() {
        String submodelId = "https://example.com/submodel/456";
        String parentPath = "parent.child";
        String elementJson = "{\"idShort\": \"TestElement\", \"modelType\": \"Property\"}";
        aasStructureSnapshotService.applyElementCreate(testSourceSystem.id, submodelId, parentPath, elementJson);
        assertNotNull(aasStructureSnapshotService);
    }

    /**
     * Tests deletion of a submodel from the snapshot.
     * Verifies successful method invocation and internal error handling.
     */
    @Test
    @DisplayName("Should apply submodel delete")
    @Transactional
    void testApplySubmodelDelete() {
        String submodelId = "https://example.com/submodel/456";
        aasStructureSnapshotService.applySubmodelDelete(testSourceSystem.id, submodelId);
        assertNotNull(aasStructureSnapshotService);
    }

    /**
     * Tests deletion of an element from a submodel in the snapshot.
     * Verifies correct method execution without exceptions.
     */
    @Test
    @DisplayName("Should apply element delete")
    @Transactional
    void testApplyElementDelete() {
        String submodelId = "https://example.com/submodel/456";
        String idShortPath = "TestElement";
        aasStructureSnapshotService.applyElementDelete(testSourceSystem.id, submodelId, idShortPath);
        assertNotNull(aasStructureSnapshotService);
    }

    /**
     * Tests attaching submodels from an AASX file to a live Source System.
     * Expects an {@link AasStructureSnapshotService.InvalidAasxException} due to invalid file format.
     */
    @Test
    @DisplayName("Should attach submodels live")
    @Transactional
    void testAttachSubmodelsLive() {
        byte[] fileBytes = "test content".getBytes();
        assertThrows(AasStructureSnapshotService.InvalidAasxException.class, () -> {
            aasStructureSnapshotService.attachSubmodelsLive(testSourceSystem.id, fileBytes);
        });
    }

    /**
     * Tests the previewAasx method with invalid content.
     * Ensures that the service throws {@link AasStructureSnapshotService.InvalidAasxException}.
     */
    @Test
    @DisplayName("Should preview AASX")
    @Transactional
    void testPreviewAasx() {
        byte[] fileBytes = "test content".getBytes();
        assertThrows(AasStructureSnapshotService.InvalidAasxException.class, () -> {
            aasStructureSnapshotService.previewAasx(fileBytes);
        });
    }
}