package de.unistuttgart.stayinsync.core.configuration.domain.entities.aas;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("AasElementLite Entity Tests")
class AasElementLiteTest {

    @PersistenceContext
    EntityManager entityManager;

    private AasElementLite elementLite;
    private AasSubmodelLite submodelLite;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test submodel
        submodelLite = new AasSubmodelLite();
        submodelLite.submodelIdShort = "TestSubmodel";
        submodelLite.submodelId = "https://test.com/submodel";
        submodelLite.persist();

        // Create test element
        elementLite = new AasElementLite();
        elementLite.submodelLite = submodelLite;
        elementLite.idShort = "TestElement";
        elementLite.modelType = "Property";
        elementLite.valueType = "xs:string";
        elementLite.idShortPath = "TestElement";
        elementLite.hasChildren = false;
        // Note: AasElementLite doesn't have a 'value' field, removing this line
    }

    @Test
    @DisplayName("Should create AasElementLite with all required fields")
    @Transactional
    void testCreateAasElementLite() {
        // Act
        elementLite.persist();
        entityManager.flush();

        // Assert
        assertNotNull(elementLite.id);
        assertEquals("TestElement", elementLite.idShort);
        assertEquals("Property", elementLite.modelType);
        assertEquals("xs:string", elementLite.valueType);
        assertEquals("TestElement", elementLite.idShortPath);
        assertFalse(elementLite.hasChildren);
        // Note: AasElementLite doesn't have a 'value' field, removing this assertion
        assertNotNull(elementLite.submodelLite);
        assertEquals(submodelLite.id, elementLite.submodelLite.id);
    }

    @Test
    @DisplayName("Should handle null optional fields")
    @Transactional
    void testNullOptionalFields() {
        // Arrange
        elementLite.valueType = null;
        elementLite.idShortPath = null;

        // Act
        elementLite.persist();
        entityManager.flush();

        // Assert
        assertNotNull(elementLite.id);
        assertEquals("TestElement", elementLite.idShort);
        assertEquals("Property", elementLite.modelType);
        assertNull(elementLite.valueType);
        // Note: AasElementLite doesn't have a 'value' field, removing this assertion
        assertNull(elementLite.idShortPath);
    }

    @Test
    @DisplayName("Should enforce unique constraint on submodel and idShortPath")
    @Transactional
    void testUniqueConstraint() {
        // Arrange
        elementLite.persist();
        entityManager.flush();

        // Create duplicate element with same submodel and idShortPath
        AasElementLite duplicateElement = new AasElementLite();
        duplicateElement.submodelLite = submodelLite;
        duplicateElement.idShort = "TestElement";
        duplicateElement.modelType = "Property";
        duplicateElement.idShortPath = "TestElement";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            duplicateElement.persist();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("Should handle different model types")
    @Transactional
    void testDifferentModelTypes() {
        // Test Property
        elementLite.modelType = "Property";
        elementLite.persist();
        entityManager.flush();
        assertEquals("Property", elementLite.modelType);

        // Test SubmodelElementCollection
        AasElementLite collectionElement = new AasElementLite();
        collectionElement.submodelLite = submodelLite;
        collectionElement.idShort = "TestCollection";
        collectionElement.modelType = "SubmodelElementCollection";
        collectionElement.idShortPath = "TestCollection";
        collectionElement.hasChildren = true;
        collectionElement.persist();
        entityManager.flush();
        assertEquals("SubmodelElementCollection", collectionElement.modelType);
        assertTrue(collectionElement.hasChildren);
    }

    @Test
    @DisplayName("Should handle cascade operations with submodel")
    @Transactional
    void testCascadeOperations() {
        // Act
        elementLite.persist();
        entityManager.flush();

        // Assert
        assertNotNull(elementLite.submodelLite);
        assertEquals(submodelLite.id, elementLite.submodelLite.id);
        
        // Test that submodel can be accessed through element
        AasSubmodelLite retrievedSubmodel = elementLite.submodelLite;
        assertNotNull(retrievedSubmodel);
        assertEquals("TestSubmodel", retrievedSubmodel.submodelIdShort);
    }
}
