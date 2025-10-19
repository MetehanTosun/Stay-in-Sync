package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;

@QuarkusTest
@DisplayName("SourceSystemEndpoint Entity Tests")
class SourceSystemEndpointTest {

    @PersistenceContext
    EntityManager entityManager;

    private SourceSystemEndpoint endpoint;
    private SourceSystem sourceSystem;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test source system
        sourceSystem = new SourceSystem();
        sourceSystem.name = "TestSourceSystem";
        sourceSystem.apiUrl = "https://source-api.com";
        sourceSystem.apiType = "REST";
        sourceSystem.persist();

        // Create test endpoint
        endpoint = new SourceSystemEndpoint();
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        endpoint.endpointPath = "/api/test";
        endpoint.httpRequestType = "GET";
        endpoint.description = "Test endpoint";
        endpoint.jsonSchema = "{\"type\": \"object\"}";
        endpoint.requestBodySchema = "{\"type\": \"object\"}";
        endpoint.responseBodySchema = "{\"type\": \"object\"}";
        endpoint.responseDts = "interface TestResponse {}";
    }

    @Test
    @DisplayName("Should create SourceSystemEndpoint with all fields")
    @Transactional
    void testCreateSourceSystemEndpoint() {
        // Act
        endpoint.persist();
        entityManager.flush();

        // Assert
        assertNotNull(endpoint.id);
        assertEquals("/api/test", endpoint.endpointPath);
        assertEquals("GET", endpoint.httpRequestType);
        assertEquals("Test endpoint", endpoint.description);
        assertEquals("{\"type\": \"object\"}", endpoint.jsonSchema);
        assertEquals("{\"type\": \"object\"}", endpoint.requestBodySchema);
        assertEquals("{\"type\": \"object\"}", endpoint.responseBodySchema);
        assertEquals("interface TestResponse {}", endpoint.responseDts);
        assertNotNull(endpoint.sourceSystem);
        assertEquals(sourceSystem.id, endpoint.sourceSystem.id);
    }

    @Test
    @DisplayName("Should handle null optional fields")
    @Transactional
    void testNullOptionalFields() {
        // Arrange
        endpoint.description = null;
        endpoint.jsonSchema = null;
        endpoint.requestBodySchema = null;
        endpoint.responseBodySchema = null;
        endpoint.responseDts = null;

        // Act
        endpoint.persist();
        entityManager.flush();

        // Assert
        assertNotNull(endpoint.id);
        assertEquals("/api/test", endpoint.endpointPath);
        assertEquals("GET", endpoint.httpRequestType);
        assertNull(endpoint.description);
        assertNull(endpoint.jsonSchema);
        assertNull(endpoint.requestBodySchema);
        assertNull(endpoint.responseBodySchema);
        assertNull(endpoint.responseDts);
        assertNotNull(endpoint.sourceSystem);
    }

    @Test
    @DisplayName("Should enforce unique constraint on sync system, endpoint path and http request type")
    @Transactional
    void testUniqueConstraint() {
        // Arrange
        endpoint.persist();
        entityManager.flush();

        // Create duplicate endpoint with same sync system, path and method
        SourceSystemEndpoint duplicateEndpoint = new SourceSystemEndpoint();
        duplicateEndpoint.sourceSystem = sourceSystem;
        duplicateEndpoint.syncSystem = sourceSystem;
        duplicateEndpoint.endpointPath = "/api/test";
        duplicateEndpoint.httpRequestType = "GET";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            duplicateEndpoint.persist();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("Should handle different HTTP request types")
    @Transactional
    void testDifferentHttpRequestTypes() {
        // Test GET
        endpoint.httpRequestType = "GET";
        endpoint.persist();
        entityManager.flush();
        assertEquals("GET", endpoint.httpRequestType);

        // Test POST
        SourceSystemEndpoint postEndpoint = new SourceSystemEndpoint();
        postEndpoint.sourceSystem = sourceSystem;
        postEndpoint.endpointPath = "/api/post";
        postEndpoint.httpRequestType = "POST";
        postEndpoint.persist();
        entityManager.flush();
        assertEquals("POST", postEndpoint.httpRequestType);

        // Test PUT
        SourceSystemEndpoint putEndpoint = new SourceSystemEndpoint();
        putEndpoint.sourceSystem = sourceSystem;
        putEndpoint.endpointPath = "/api/put";
        putEndpoint.httpRequestType = "PUT";
        putEndpoint.persist();
        entityManager.flush();
        assertEquals("PUT", putEndpoint.httpRequestType);

        // Test DELETE
        SourceSystemEndpoint deleteEndpoint = new SourceSystemEndpoint();
        deleteEndpoint.sourceSystem = sourceSystem;
        deleteEndpoint.endpointPath = "/api/delete";
        deleteEndpoint.httpRequestType = "DELETE";
        deleteEndpoint.persist();
        entityManager.flush();
        assertEquals("DELETE", deleteEndpoint.httpRequestType);
    }

    @Test
    @DisplayName("Should maintain relationship with source system")
    @Transactional
    void testRelationshipWithSourceSystem() {
        // Act
        endpoint.persist();
        entityManager.flush();

        // Assert
        assertNotNull(endpoint.sourceSystem);
        assertEquals(sourceSystem.id, endpoint.sourceSystem.id);
        assertEquals("TestSourceSystem", endpoint.sourceSystem.name);
        
        // Test that source system can be accessed through endpoint
        SourceSystem retrievedSystem = endpoint.sourceSystem;
        assertNotNull(retrievedSystem);
        assertEquals("TestSourceSystem", retrievedSystem.name);
    }

    @Test
    @DisplayName("Should handle long endpoint paths")
    @Transactional
    void testLongEndpointPaths() {
        // Arrange
        String longPath = "/api/very/long/path/to/endpoint/with/many/segments";
        endpoint.endpointPath = longPath;

        // Act
        endpoint.persist();
        entityManager.flush();

        // Assert
        assertNotNull(endpoint.id);
        assertEquals(longPath, endpoint.endpointPath);
    }

    @Test
    @DisplayName("Should handle complex JSON schemas")
    @Transactional
    void testComplexJsonSchemas() {
        // Arrange
        String complexSchema = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, \"age\": {\"type\": \"number\"}}, \"required\": [\"name\"]}";
        endpoint.jsonSchema = complexSchema;

        // Act
        endpoint.persist();
        entityManager.flush();

        // Assert
        assertNotNull(endpoint.id);
        assertEquals(complexSchema, endpoint.jsonSchema);
    }
}
