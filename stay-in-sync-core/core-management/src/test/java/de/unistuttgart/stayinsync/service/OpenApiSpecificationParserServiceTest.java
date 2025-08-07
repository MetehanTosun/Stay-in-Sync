package de.unistuttgart.stayinsync.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.OpenApiSpecificationParserService;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import de.unistuttgart.stayinsync.rest.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OpenApiSpecificationParserServiceTest extends BaseTest {

    @Inject
    OpenApiSpecificationParserService openApiSpecificationParserService;

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    EntityManager entityManager;

    private SourceSystem testSourceSystem;

    @BeforeEach
    @Transactional
    public void setup() {
        // Create a test source system
        CreateSourceSystemDTO sourceSystemDTO = new CreateSourceSystemDTO(
                null, "TestSystem", "http://localhost/test", "Test system for OpenAPI parsing", 
                "REST", null, null, null
        );
        sourceSystemService.createSourceSystem(sourceSystemDTO);
        
        // Find the created source system
        List<SourceSystem> all = sourceSystemService.findAllSourceSystems();
        testSourceSystem = all.stream()
                .filter(ss -> "TestSystem".equals(ss.name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test source system not found"));
    }

    /**
     * Test parsing a simple OpenAPI spec with basic endpoint definition.
     */
    @Test
    @Transactional
    public void testParseSimpleOpenApiSpec() {
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Simple Test API",
                "version": "1.0.0"
              },
              "paths": {
                "/users": {
                  "get": {
                    "summary": "Get all users",
                    "responses": {
                      "200": {
                        "description": "Successful response",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "array",
                              "items": {
                                "type": "object",
                                "properties": {
                                  "id": {"type": "integer"},
                                  "name": {"type": "string"},
                                  "email": {"type": "string"}
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        // Update the source system with OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/users", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("GET", endpoint.httpRequestType, "HTTP method should match");
        
        assertNotNull(endpoint.responseBodySchema, "Response body schema should be extracted");
        assertTrue(endpoint.responseBodySchema.contains("array"), "Response body schema should contain array type");
        assertTrue(endpoint.responseBodySchema.contains("id"), "Response body schema should contain id field");
        assertTrue(endpoint.responseBodySchema.contains("name"), "Response body schema should contain name field");
        assertTrue(endpoint.responseBodySchema.contains("email"), "Response body schema should contain email field");
    }

    /**
     * Test parsing OpenAPI spec with multiple endpoints and different HTTP methods.
     */
    @Test
    @Transactional
    public void testParseMultipleEndpoints() {
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Multi-Endpoint API",
                "version": "1.0.0"
              },
              "paths": {
                "/users": {
                  "get": {
                    "summary": "Get all users",
                    "responses": {
                      "200": {
                        "description": "Successful response",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "array",
                              "items": {"type": "object"}
                            }
                          }
                        }
                      }
                    }
                  },
                  "post": {
                    "summary": "Create user",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "name": {"type": "string"},
                              "email": {"type": "string"}
                            }
                          }
                        }
                      }
                    },
                    "responses": {
                      "201": {
                        "description": "User created"
                      }
                    }
                  }
                },
                "/users/{id}": {
                  "get": {
                    "summary": "Get user by ID",
                    "parameters": [
                      {
                        "name": "id",
                        "in": "path",
                        "required": true,
                        "schema": {"type": "integer"}
                      }
                    ],
                    "responses": {
                      "200": {
                        "description": "Successful response"
                      }
                    }
                  }
                }
              }
            }
            """;

        // Update the source system with OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that all endpoints were created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(3, endpoints.size(), "Should create three endpoints");

        // Verify GET /users
        SourceSystemEndpoint getUsers = endpoints.stream()
                .filter(e -> "/users".equals(e.endpointPath) && "GET".equals(e.httpRequestType))
                .findFirst()
                .orElse(null);
        assertNotNull(getUsers, "GET /users endpoint should exist");
        assertNotNull(getUsers.responseBodySchema, "GET /users should have response schema");

        // Verify POST /users
        SourceSystemEndpoint postUsers = endpoints.stream()
                .filter(e -> "/users".equals(e.endpointPath) && "POST".equals(e.httpRequestType))
                .findFirst()
                .orElse(null);
        assertNotNull(postUsers, "POST /users endpoint should exist");
        assertNotNull(postUsers.requestBodySchema, "POST /users should have request schema");

        // Verify GET /users/{id}
        SourceSystemEndpoint getUserById = endpoints.stream()
                .filter(e -> "/users/{id}".equals(e.endpointPath) && "GET".equals(e.httpRequestType))
                .findFirst()
                .orElse(null);
        assertNotNull(getUserById, "GET /users/{id} endpoint should exist");
    }

    /**
     * Test parsing OpenAPI spec with security schemes (API keys and authorization).
     */
    @Test
    @Transactional
    public void testParseWithSecuritySchemes() {
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Secure API",
                "version": "1.0.0"
              },
              "components": {
                "securitySchemes": {
                  "ApiKeyAuth": {
                    "type": "apiKey",
                    "in": "header",
                    "name": "X-API-Key"
                  },
                  "BearerAuth": {
                    "type": "http",
                    "scheme": "bearer"
                  }
                }
              },
              "paths": {
                "/secure": {
                  "get": {
                    "summary": "Secure endpoint",
                    "responses": {
                      "200": {
                        "description": "Successful response"
                      }
                    }
                  }
                }
              }
            }
            """;

        // Update the source system with OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        // Note: Security schemes are processed separately and would create API headers
        // This test focuses on endpoint creation with security schemes present
        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/secure", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("GET", endpoint.httpRequestType, "HTTP method should match");
    }

    /**
     * Test parsing OpenAPI spec with query parameters.
     */
    @Test
    @Transactional
    public void testParseWithQueryParameters() {
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Query Parameters API",
                "version": "1.0.0"
              },
              "paths": {
                "/search": {
                  "get": {
                    "summary": "Search with parameters",
                    "parameters": [
                      {
                        "name": "query",
                        "in": "query",
                        "required": true,
                        "schema": {"type": "string"}
                      },
                      {
                        "name": "limit",
                        "in": "query",
                        "required": false,
                        "schema": {"type": "integer"}
                      },
                      {
                        "name": "active",
                        "in": "query",
                        "required": false,
                        "schema": {"type": "boolean"}
                      }
                    ],
                    "responses": {
                      "200": {
                        "description": "Successful response"
                      }
                    }
                  }
                }
              }
            }
            """;

        // Update the source system with OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/search", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("GET", endpoint.httpRequestType, "HTTP method should match");
    }

    /**
     * Test parsing OpenAPI spec with complex request/response schemas.
     */
    @Test
    @Transactional
    public void testParseWithComplexSchemas() {
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Complex Schema API",
                "version": "1.0.0"
              },
              "components": {
                "schemas": {
                  "User": {
                    "type": "object",
                    "properties": {
                      "id": {"type": "integer"},
                      "name": {"type": "string"},
                      "profile": {"$ref": "#/components/schemas/Profile"}
                    }
                  },
                  "Profile": {
                    "type": "object",
                    "properties": {
                      "avatar": {"type": "string"},
                      "preferences": {"type": "object"}
                    }
                  }
                }
              },
              "paths": {
                "/user": {
                  "post": {
                    "summary": "Create user",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": {"$ref": "#/components/schemas/User"}
                        }
                      }
                    },
                    "responses": {
                      "200": {
                        "description": "User created",
                        "content": {
                          "application/json": {
                            "schema": {"$ref": "#/components/schemas/User"}
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        // Update the source system with OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/user", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("POST", endpoint.httpRequestType, "HTTP method should match");
        
        assertNotNull(endpoint.requestBodySchema, "Request body schema should be extracted");
        assertNotNull(endpoint.responseBodySchema, "Response body schema should be extracted");
        assertTrue(endpoint.requestBodySchema.contains("User"), "Request schema should contain User reference");
        assertTrue(endpoint.responseBodySchema.contains("User"), "Response schema should contain User reference");
    }

    /**
     * Test handling of invalid OpenAPI spec.
     */
    @Test
    @Transactional
    public void testHandleInvalidOpenApiSpec() {
        String invalidOpenApiSpec = "invalid json content";

        // Update the source system with invalid OpenAPI spec
        testSourceSystem.openApiSpec = invalidOpenApiSpec;
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec should not throw exception but handle gracefully
        assertDoesNotThrow(() -> {
            openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);
        }, "Should handle invalid OpenAPI spec gracefully");

        // Verify that no endpoints were created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(0, endpoints.size(), "Should not create endpoints from invalid spec");
    }

    /**
     * Test handling of null/empty OpenAPI spec.
     */
    @Test
    @Transactional
    public void testHandleNullOpenApiSpec() {
        // Update the source system with null OpenAPI spec
        testSourceSystem.openApiSpec = null;
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec should handle null gracefully
        assertDoesNotThrow(() -> {
            openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);
        }, "Should handle null OpenAPI spec gracefully");

        // Verify that no endpoints were created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(0, endpoints.size(), "Should not create endpoints from null spec");
    }

    /**
     * Test handling of empty OpenAPI spec.
     */
    @Test
    @Transactional
    public void testHandleEmptyOpenApiSpec() {
        // Update the source system with empty OpenAPI spec
        testSourceSystem.openApiSpec = "";
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec should handle empty gracefully
        assertDoesNotThrow(() -> {
            openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);
        }, "Should handle empty OpenAPI spec gracefully");

        // Verify that no endpoints were created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(0, endpoints.size(), "Should not create endpoints from empty spec");
    }

    /**
     * Test parsing OpenAPI spec with non-JSON response content.
     */
    @Test
    @Transactional
    public void testParseWithNonJsonResponse() {
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Non-JSON API",
                "version": "1.0.0"
              },
              "paths": {
                "/download": {
                  "get": {
                    "summary": "Download file",
                    "responses": {
                      "200": {
                        "description": "File download",
                        "content": {
                          "application/octet-stream": {
                            "schema": {
                              "type": "string",
                              "format": "binary"
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        // Update the source system with OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        entityManager.merge(testSourceSystem);
        entityManager.flush();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created but without response schema (only JSON is supported)
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/download", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("GET", endpoint.httpRequestType, "HTTP method should match");
        
        assertNull(endpoint.responseBodySchema, "Response body schema should be null for non-JSON content");
    }
} 