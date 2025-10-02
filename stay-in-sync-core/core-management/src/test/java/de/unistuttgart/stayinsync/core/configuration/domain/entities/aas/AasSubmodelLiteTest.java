package de.unistuttgart.stayinsync.core.configuration.domain.entities.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("AasSubmodelLite Entity Tests")
class AasSubmodelLiteTest {

    @PersistenceContext
    EntityManager entityManager;

    private AasSubmodelLite submodelLite;
    private SourceSystem sourceSystem;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test source system
        sourceSystem = new SourceSystem();
        sourceSystem.name = "TestSourceSystem";
        sourceSystem.apiUrl = "https://test-api.com";
        sourceSystem.apiType = "REST";
        sourceSystem.persist();

        // Create test submodel
        submodelLite = new AasSubmodelLite();
        submodelLite.sourceSystem = sourceSystem;
        submodelLite.submodelId = "https://test.com/submodel";
        submodelLite.submodelIdShort = "TestSubmodel";
        submodelLite.semanticId = "https://semantic.com/submodel";
        submodelLite.kind = "Instance";
    }

    @Test
    @DisplayName("Should create AasSubmodelLite with all required fields")
    @Transactional
    void testCreateAasSubmodelLite() {
        // Act
        submodelLite.persist();
        entityManager.flush();

        // Assert
        assertNotNull(submodelLite.id);
        assertEquals("https://test.com/submodel", submodelLite.submodelId);
        assertEquals("TestSubmodel", submodelLite.submodelIdShort);
        assertEquals("https://semantic.com/submodel", submodelLite.semanticId);
        assertEquals("Instance", submodelLite.kind);
        assertNotNull(submodelLite.sourceSystem);
        assertEquals(sourceSystem.id, submodelLite.sourceSystem.id);
    }

    @Test
    @DisplayName("Should handle null optional fields")
    @Transactional
    void testNullOptionalFields() {
        // Arrange
        submodelLite.submodelIdShort = null;
        submodelLite.semanticId = null;
        submodelLite.kind = null;

        // Act
        submodelLite.persist();
        entityManager.flush();

        // Assert
        assertNotNull(submodelLite.id);
        assertEquals("https://test.com/submodel", submodelLite.submodelId);
        assertNull(submodelLite.submodelIdShort);
        assertNull(submodelLite.semanticId);
        assertNull(submodelLite.kind);
        assertNotNull(submodelLite.sourceSystem);
    }

    @Test
    @DisplayName("Should enforce unique constraint on source system and submodelId")
    @Transactional
    void testUniqueConstraint() {
        // Arrange
        submodelLite.persist();
        entityManager.flush();

        // Create duplicate submodel with same source system and submodelId
        AasSubmodelLite duplicateSubmodel = new AasSubmodelLite();
        duplicateSubmodel.sourceSystem = sourceSystem;
        duplicateSubmodel.submodelId = "https://test.com/submodel";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            duplicateSubmodel.persist();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("Should handle different kind values")
    @Transactional
    void testDifferentKindValues() {
        // Test Instance
        submodelLite.kind = "Instance";
        submodelLite.persist();
        entityManager.flush();
        assertEquals("Instance", submodelLite.kind);

        // Test Template
        AasSubmodelLite templateSubmodel = new AasSubmodelLite();
        templateSubmodel.sourceSystem = sourceSystem;
        templateSubmodel.submodelId = "https://test.com/template";
        templateSubmodel.kind = "Template";
        templateSubmodel.persist();
        entityManager.flush();
        assertEquals("Template", templateSubmodel.kind);
    }

    @Test
    @DisplayName("Should maintain relationship with source system")
    @Transactional
    void testRelationshipWithSourceSystem() {
        // Act
        submodelLite.persist();
        entityManager.flush();

        // Assert
        assertNotNull(submodelLite.sourceSystem);
        assertEquals(sourceSystem.id, submodelLite.sourceSystem.id);
        assertEquals("TestSourceSystem", submodelLite.sourceSystem.name);
        
        // Test that source system can be accessed through submodel
        SourceSystem retrievedSystem = submodelLite.sourceSystem;
        assertNotNull(retrievedSystem);
        assertEquals("TestSourceSystem", retrievedSystem.name);
    }

    @Test
    @DisplayName("Should handle long submodel IDs")
    @Transactional
    void testLongSubmodelIds() {
        // Arrange
        String longSubmodelId = "https://very-long-domain-name.com/very/long/path/to/submodel/with/many/segments";
        submodelLite.submodelId = longSubmodelId;

        // Act
        submodelLite.persist();
        entityManager.flush();

        // Assert
        assertNotNull(submodelLite.id);
        assertEquals(longSubmodelId, submodelLite.submodelId);
    }

    @Test
    @DisplayName("Should handle special characters in submodel ID short")
    @Transactional
    void testSpecialCharactersInSubmodelIdShort() {
        // Arrange
        submodelLite.submodelIdShort = "Test-Submodel_123";

        // Act
        submodelLite.persist();
        entityManager.flush();

        // Assert
        assertNotNull(submodelLite.id);
        assertEquals("Test-Submodel_123", submodelLite.submodelIdShort);
    }
}
