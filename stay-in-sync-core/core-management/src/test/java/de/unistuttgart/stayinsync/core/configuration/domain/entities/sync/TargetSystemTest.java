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
@DisplayName("TargetSystem Entity Tests")
class TargetSystemTest {

    @PersistenceContext
    EntityManager entityManager;

    private TargetSystem targetSystem;
    private TargetSystemEndpoint endpoint1;
    private TargetSystemEndpoint endpoint2;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test target system
        targetSystem = new TargetSystem();
        targetSystem.name = "TestTargetSystem";
        targetSystem.apiUrl = "https://target-api.com";
        targetSystem.apiType = "REST";
        targetSystem.description = "Test target system for unit testing";
        targetSystem.syncSystemEndpoints = new HashSet<>();
        
        // Initialize endpoints
        endpoint1 = new TargetSystemEndpoint();
        endpoint2 = new TargetSystemEndpoint();
    }

    @Test
    @DisplayName("Should create TargetSystem with basic properties")
    @Transactional
    void testCreateTargetSystem() {
        // Act
        targetSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(targetSystem.id);
        assertEquals("TestTargetSystem", targetSystem.name);
        assertEquals("https://target-api.com", targetSystem.apiUrl);
        assertEquals("REST", targetSystem.apiType);
        assertEquals("Test target system for unit testing", targetSystem.description);
        assertNotNull(targetSystem.syncSystemEndpoints);
        assertTrue(targetSystem.syncSystemEndpoints.isEmpty());
    }

    @Test
    @DisplayName("Should handle cascade operations with endpoints")
    @Transactional
    void testCascadeOperationsWithEndpoints() {
        // Arrange
        endpoint1.endpointPath = "/api/target1";
        endpoint1.httpRequestType = "GET";
        endpoint1.syncSystem = targetSystem;

        endpoint2.endpointPath = "/api/target2";
        endpoint2.httpRequestType = "POST";
        endpoint2.syncSystem = targetSystem;

        targetSystem.syncSystemEndpoints.add(endpoint1);
        targetSystem.syncSystemEndpoints.add(endpoint2);

        // Act
        targetSystem.persist();
        endpoint1.persist();
        endpoint2.persist();
        entityManager.flush();

        // Assert
        assertNotNull(targetSystem.id);
        assertEquals(2, targetSystem.syncSystemEndpoints.size());
        assertTrue(targetSystem.syncSystemEndpoints.contains(endpoint1));
        assertTrue(targetSystem.syncSystemEndpoints.contains(endpoint2));
        
        // Verify endpoints are persisted
        assertNotNull(endpoint1.id);
        assertNotNull(endpoint2.id);
        assertEquals(targetSystem.id, endpoint1.syncSystem.id);
        assertEquals(targetSystem.id, endpoint2.syncSystem.id);
    }

    @Test
    @DisplayName("Should handle null optional fields")
    @Transactional
    void testNullOptionalFields() {
        // Arrange
        targetSystem.description = null;

        // Act
        targetSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(targetSystem.id);
        assertEquals("TestTargetSystem", targetSystem.name);
        assertEquals("https://target-api.com", targetSystem.apiUrl);
        assertEquals("REST", targetSystem.apiType);
        assertNull(targetSystem.description);
    }

    @Test
    @DisplayName("Should handle empty endpoints collection")
    @Transactional
    void testEmptyEndpointsCollection() {
        // Act
        targetSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(targetSystem.id);
        assertNotNull(targetSystem.syncSystemEndpoints);
        assertTrue(targetSystem.syncSystemEndpoints.isEmpty());
    }

    @Test
    @DisplayName("Should handle different API types")
    @Transactional
    void testDifferentApiTypes() {
        // Test REST API
        targetSystem.apiType = "REST";
        targetSystem.persist();
        entityManager.flush();
        assertEquals("REST", targetSystem.apiType);

        // Test GraphQL API
        TargetSystem graphqlSystem = new TargetSystem();
        graphqlSystem.name = "GraphQLTargetSystem";
        graphqlSystem.apiUrl = "https://graphql-target.com";
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
        endpoint1 = new TargetSystemEndpoint();
        endpoint1.endpointPath = "/api/target";
        endpoint1.httpRequestType = "GET";
        endpoint1.syncSystem = (SyncSystem) targetSystem;

        targetSystem.syncSystemEndpoints.add(endpoint1);

        // Act
        targetSystem.persist();
        entityManager.flush();

        // Assert
        assertEquals(targetSystem, endpoint1.syncSystem);
        assertTrue(targetSystem.syncSystemEndpoints.contains(endpoint1));
        
        // Test that we can navigate from endpoint to target system
        TargetSystem retrievedSystem = (TargetSystem) endpoint1.syncSystem;
        assertNotNull(retrievedSystem);
        assertEquals(targetSystem.id, retrievedSystem.id);
        assertEquals("TestTargetSystem", retrievedSystem.name);
    }

    @Test
    @DisplayName("Should handle long API URLs")
    @Transactional
    void testLongApiUrls() {
        // Arrange
        String longApiUrl = "https://very-long-domain-name.com/very/long/path/to/api/endpoint";
        targetSystem.apiUrl = longApiUrl;

        // Act
        targetSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(targetSystem.id);
        assertEquals(longApiUrl, targetSystem.apiUrl);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    @Transactional
    void testSpecialCharactersInName() {
        // Arrange
        targetSystem.name = "Test-Target_System 123";

        // Act
        targetSystem.persist();
        entityManager.flush();

        // Assert
        assertNotNull(targetSystem.id);
        assertEquals("Test-Target_System 123", targetSystem.name);
    }
}
