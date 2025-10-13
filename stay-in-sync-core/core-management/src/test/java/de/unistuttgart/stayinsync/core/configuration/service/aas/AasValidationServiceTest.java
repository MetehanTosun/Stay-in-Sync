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

@QuarkusTest
public class AasValidationServiceTest {

    @Inject
    AasValidationService validationService;

    private SourceSystem sourceSystem;
    private TargetSystem targetSystem;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up in correct order to avoid foreign key constraints
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

    @Test
    void testValidateAasSource_ValidSourceSystem() {
        // When
        SourceSystem result = validationService.validateAasSource(sourceSystem);

        // Then
        assertNotNull(result);
        assertEquals(sourceSystem.id, result.id);
        assertEquals(sourceSystem.name, result.name);
    }

    @Test
    void testValidateAasSource_NullSourceSystem() {
        // When & Then
        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasSource(null);
        });
    }

    @Test
    @Transactional
    void testValidateAasSource_NonAasSourceSystem() {
        // Given
        SourceSystem nonAasSystem = new SourceSystem();
        nonAasSystem.apiType = "REST";
        nonAasSystem.apiUrl = "http://rest.example";
        nonAasSystem.name = "non-aas-source";
        nonAasSystem.persist();

        // When & Then
        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasSource(nonAasSystem);
        });
    }

    @Test
    void testValidateAasTarget_ValidTargetSystem() {
        // When
        TargetSystem result = validationService.validateAasTarget(targetSystem);

        // Then
        assertNotNull(result);
        assertEquals(targetSystem.id, result.id);
        assertEquals(targetSystem.name, result.name);
    }

    @Test
    void testValidateAasTarget_NullTargetSystem() {
        // When & Then
        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasTarget(null);
        });
    }

    @Test
    @Transactional
    void testValidateAasTarget_NonAasTargetSystem() {
        // Given
        TargetSystem nonAasSystem = new TargetSystem();
        nonAasSystem.apiType = "REST";
        nonAasSystem.apiUrl = "http://rest.example";
        nonAasSystem.name = "non-aas-target";
        nonAasSystem.persist();

        // When & Then
        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasTarget(nonAasSystem);
        });
    }

    @Test
    @Transactional
    void testValidateAasSource_WithNullApiType() {
        // Given
        SourceSystem invalidSystem = new SourceSystem();
        invalidSystem.apiType = null;
        invalidSystem.apiUrl = "http://aas.example";
        invalidSystem.name = "invalid-source";
        invalidSystem.persist();

        // When & Then
        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasSource(invalidSystem);
        });
    }

    @Test
    @Transactional
    void testValidateAasTarget_WithEmptyApiType() {
        // Given
        TargetSystem invalidSystem = new TargetSystem();
        invalidSystem.apiType = "";
        invalidSystem.apiUrl = "http://aas.example";
        invalidSystem.name = "invalid-target";
        invalidSystem.persist();

        // When & Then
        assertThrows(CoreManagementException.class, () -> {
            validationService.validateAasTarget(invalidSystem);
        });
    }
}
