package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("SourceSystem Entity Tests")
class SourceSystemTest {

    @PersistenceContext
    EntityManager entityManager;

    private SourceSystem sourceSystem;
    private SourceSystemEndpoint endpoint1;
    private SourceSystemEndpoint endpoint2;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test source system
        sourceSystem = new SourceSystem();
        sourceSystem.name = "TestSourceSystem";
        sourceSystem.apiUrl = "https://test-api.com";
        sourceSystem.apiType = "REST";
        sourceSystem.description = "Test source system for unit testing";
        sourceSystem.sourceSystemEndpoints = new HashSet<>();
    }

    @Test
    @DisplayName("Should create SourceSystem with basic properties")
    @Transactional
    void testCreateSourceSystem() {
        // Act
        sourceSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(sourceSystem.id);
        assertEquals("TestSourceSystem", sourceSystem.name);
        assertEquals("https://test-api.com", sourceSystem.apiUrl);
        assertEquals("REST", sourceSystem.apiType);
        assertEquals("Test source system for unit testing", sourceSystem.description);
        assertNotNull(sourceSystem.sourceSystemEndpoints);
        assertTrue(sourceSystem.sourceSystemEndpoints.isEmpty());
    }

    @Test
    @DisplayName("Should handle cascade operations with endpoints")
    @Transactional
    void testCascadeOperationsWithEndpoints() {
        // Arrange
        endpoint1 = new SourceSystemEndpoint();
        endpoint1.endpointPath = "/api/test1";
        endpoint1.httpRequestType = "GET";
        endpoint1.sourceSystem = sourceSystem;

        endpoint2 = new SourceSystemEndpoint();
        endpoint2.endpointPath = "/api/test2";
        endpoint2.httpRequestType = "POST";
        endpoint2.sourceSystem = sourceSystem;

        sourceSystem.sourceSystemEndpoints.add(endpoint1);
        sourceSystem.sourceSystemEndpoints.add(endpoint2);

        // Act
        sourceSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(sourceSystem.id);
        assertEquals(2, sourceSystem.sourceSystemEndpoints.size());
        assertTrue(sourceSystem.sourceSystemEndpoints.contains(endpoint1));
        assertTrue(sourceSystem.sourceSystemEndpoints.contains(endpoint2));
        
        // Verify endpoints are persisted
        assertNotNull(endpoint1.id);
        assertNotNull(endpoint2.id);
        assertEquals(sourceSystem.id, endpoint1.sourceSystem.id);
        assertEquals(sourceSystem.id, endpoint2.sourceSystem.id);
    }

    @Test
    @DisplayName("Should handle null optional fields")
    @Transactional
    void testNullOptionalFields() {
        // Arrange
        sourceSystem.description = null;

        // Act
        sourceSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(sourceSystem.id);
        assertEquals("TestSourceSystem", sourceSystem.name);
        assertEquals("https://test-api.com", sourceSystem.apiUrl);
        assertEquals("REST", sourceSystem.apiType);
        assertNull(sourceSystem.description);
    }

    @Test
    @DisplayName("Should handle empty endpoints collection")
    @Transactional
    void testEmptyEndpointsCollection() {
        // Act
        sourceSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(sourceSystem.id);
        assertNotNull(sourceSystem.sourceSystemEndpoints);
        assertTrue(sourceSystem.sourceSystemEndpoints.isEmpty());
    }

    @Test
    @DisplayName("Should handle different API types")
    @Transactional
    void testDifferentApiTypes() {
        // Test REST API
        sourceSystem.apiType = "REST";
        sourceSystem.persist();
        entityManager.flush();
        assertEquals("REST", sourceSystem.apiType);

        // Test GraphQL API
        SourceSystem graphqlSystem = new SourceSystem();
        graphqlSystem.name = "GraphQLSystem";
        graphqlSystem.apiUrl = "https://graphql-api.com";
        graphqlSystem.apiType = "GraphQL";
        graphqlSystem.persist();
        entityManager.flush();
        assertEquals("GraphQL", graphqlSystem.apiType);
    }

    @Test
    @DisplayName("Should maintain bidirectional relationship with endpoints")
    @Transactional
    void testBidirectionalRelationship() {
        // Arrange
        endpoint1 = new SourceSystemEndpoint();
        endpoint1.endpointPath = "/api/test";
        endpoint1.httpRequestType = "GET";
        endpoint1.sourceSystem = sourceSystem;

        sourceSystem.sourceSystemEndpoints.add(endpoint1);

        // Act
        sourceSystem.persist();
        entityManager.flush();

        // Assert
        assertEquals(sourceSystem, endpoint1.sourceSystem);
        assertTrue(sourceSystem.sourceSystemEndpoints.contains(endpoint1));
        
        // Test that we can navigate from endpoint to source system
        SourceSystem retrievedSystem = endpoint1.sourceSystem;
        assertNotNull(retrievedSystem);
        assertEquals(sourceSystem.id, retrievedSystem.id);
        assertEquals("TestSourceSystem", retrievedSystem.name);
    }
}
