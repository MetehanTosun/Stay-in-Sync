package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link de.unistuttgart.stayinsync.core.configuration.service.aas.AasValidationService}.
 * Verifies validation logic for AAS-based SourceSystems and TargetSystems.
 * Tests include null checks, invalid API types, and successful validation cases.
 */
@QuarkusTest
public class AasValidationServiceTest {

    @Inject
    AasValidationService validationService;

    private SourceSystem sourceSystem;
    private TargetSystem targetSystem;

    /**
     * Initializes test data before each test case.
     * Creates and persists valid SourceSystem and TargetSystem entities
     * with appropriate AAS configuration for validation testing.
     */
    @BeforeEach
    @Transactional
    void setUp() {
        sourceSystem = new SourceSystem();
        sourceSystem.apiType = "AAS";
        sourceSystem.apiUrl = "http://aas.example";
        sourceSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        sourceSystem.name = "test-source";
        sourceSystem.persist();
        
        targetSystem = new TargetSystem();
        targetSystem.apiType = "AAS";
        targetSystem.apiUrl = "http://target-aas.example";
        targetSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvdGFyZ2V0LzAzMDBfNjE0MV81MDUyXzg3MTU";
        targetSystem.name = "test-target";
        targetSystem.persist();
    }

    /**
     * Tests validation of a valid AAS SourceSystem.
     * Expects successful validation without exceptions and matching entity fields.
     */
    @Test
    void testValidateAasSource_ValidSourceSystem() {
        SourceSystem result = validationService.validateAasSource(sourceSystem);

        assertNotNull(result);
        assertEquals(sourceSystem.id, result.id);
        assertEquals(sourceSystem.name, result.name);
    }

    /**
     * Tests validation behavior when a null SourceSystem is provided.
     * Expects a {@link CoreManagementException} to be thrown.
     */
    @Test
    void testValidateAasSource_NullSourceSystem() {
        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasSource(null);
        });
    }

    /**
     * Tests validation of a non-AAS SourceSystem.
     * Expects a {@link CoreManagementException} since the API type is invalid.
     */
    @Test
    @Transactional
    void testValidateAasSource_NonAasSourceSystem() {
        SourceSystem nonAasSystem = new SourceSystem();
        nonAasSystem.apiType = "REST";
        nonAasSystem.apiUrl = "http://rest.example";
        nonAasSystem.name = "non-aas-source";
        nonAasSystem.persist();

        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasSource(nonAasSystem);
        });
    }

    /**
     * Tests validation of a valid AAS TargetSystem.
     * Expects successful validation and correct entity return.
     */
    @Test
    void testValidateAasTarget_ValidTargetSystem() {
        TargetSystem result = validationService.validateAasTarget(targetSystem);

        assertNotNull(result);
        assertEquals(targetSystem.id, result.id);
        assertEquals(targetSystem.name, result.name);
    }

    /**
     * Tests validation behavior when a null TargetSystem is passed.
     * Expects a {@link CoreManagementException} to be thrown.
     */
    @Test
    void testValidateAasTarget_NullTargetSystem() {
        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasTarget(null);
        });
    }

    /**
     * Tests validation of a non-AAS TargetSystem.
     * Expects a {@link CoreManagementException} since the API type does not match AAS.
     */
    @Test
    @Transactional
    void testValidateAasTarget_NonAasTargetSystem() {
        TargetSystem nonAasSystem = new TargetSystem();
        nonAasSystem.apiType = "REST";
        nonAasSystem.apiUrl = "http://rest.example";
        nonAasSystem.name = "non-aas-target";
        nonAasSystem.persist();

        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasTarget(nonAasSystem);
        });
    }

    /**
     * Tests validation of a SourceSystem with a null API type.
     * Expects a {@link CoreManagementException} since the configuration is invalid.
     */
    @Test
    @Transactional
    void testValidateAasSource_WithNullApiType() {
        SourceSystem invalidSystem = new SourceSystem();
        invalidSystem.apiType = null;
        invalidSystem.apiUrl = "http://aas.example";
        invalidSystem.name = "invalid-source";
        invalidSystem.persist();

        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasSource(invalidSystem);
        });
    }

    /**
     * Tests validation of a TargetSystem with an empty API type.
     * Expects a {@link CoreManagementException} to be thrown.
     */
    @Test
    @Transactional
    void testValidateAasTarget_WithEmptyApiType() {
        TargetSystem invalidSystem = new TargetSystem();
        invalidSystem.apiType = "";
        invalidSystem.apiUrl = "http://aas.example";
        invalidSystem.name = "invalid-target";
        invalidSystem.persist();

        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasTarget(invalidSystem);
        });
    }
}
