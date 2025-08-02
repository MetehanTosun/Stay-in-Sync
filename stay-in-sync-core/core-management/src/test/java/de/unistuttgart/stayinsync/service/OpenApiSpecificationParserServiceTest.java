package de.unistuttgart.stayinsync.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.OpenApiSpecificationParserService;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OpenApiSpecificationParserServiceTest {

    @Inject
    OpenApiSpecificationParserService openApiSpecificationParserService;

    @Inject
    SourceSystemService sourceSystemService;

    private SourceSystem testSourceSystem;

    @BeforeEach
    public void setup() {
        // Cleanup existing source systems
        List<SourceSystem> all = sourceSystemService.findAllSourceSystems();
        all.forEach(ss -> sourceSystemService.deleteSourceSystemById(ss.id));

        // Create a test source system
        CreateSourceSystemDTO sourceSystemDTO = new CreateSourceSystemDTO(
                1L, "TestSystem", "http://localhost/test", null, null, null, null, null
        );
        sourceSystemService.createSourceSystem(sourceSystemDTO);
        Optional<SourceSystem> found = sourceSystemService.findSourceSystemById(1L);
        assertTrue(found.isPresent(), "Test source system should be created");
        testSourceSystem = found.get();
    }

    @Test
    public void testExtractResponseBodyFromOpenApiSpec() {
        // OpenAPI spec with response body definition
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "paths": {
                "/users": {
                  "get": {
                    "summary": "Get users",
                    "responses": {
                      "200": {
                        "description": "Successful response",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {
                                "users": {
                                  "type": "array",
                                  "items": {
                                    "type": "object",
                                    "properties": {
                                      "id": {
                                        "type": "integer"
                                      },
                                      "name": {
                                        "type": "string"
                                      },
                                      "email": {
                                        "type": "string"
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
                }
              }
            }
            """;

        // Set the OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        testSourceSystem.persist();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created with response body schema
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/users", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("GET", endpoint.httpRequestType, "HTTP method should match");
        
        assertNotNull(endpoint.responseBodySchema, "Response body schema should be extracted");
        assertTrue(endpoint.responseBodySchema.contains("users"), "Response body schema should contain users array");
        assertTrue(endpoint.responseBodySchema.contains("id"), "Response body schema should contain id field");
        assertTrue(endpoint.responseBodySchema.contains("name"), "Response body schema should contain name field");
        assertTrue(endpoint.responseBodySchema.contains("email"), "Response body schema should contain email field");
    }

    @Test
    public void testExtractResponseBodyWithNoResponseDefinition() {
        // OpenAPI spec without response body definition
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "paths": {
                "/status": {
                  "get": {
                    "summary": "Get status",
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

        // Set the OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        testSourceSystem.persist();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created without response body schema
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/status", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("GET", endpoint.httpRequestType, "HTTP method should match");
        
        assertNull(endpoint.responseBodySchema, "Response body schema should be null when no response definition");
    }

    @Test
    public void testExtractResponseBodyWithNonJsonContent() {
        // OpenAPI spec with non-JSON response content
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "paths": {
                "/download": {
                  "get": {
                    "summary": "Download file",
                    "responses": {
                      "200": {
                        "description": "Successful response",
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

        // Set the OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        testSourceSystem.persist();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created without response body schema (only JSON is supported)
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/download", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("GET", endpoint.httpRequestType, "HTTP method should match");
        
        assertNull(endpoint.responseBodySchema, "Response body schema should be null for non-JSON content");
    }

    @Test
    public void testExtractResponseBodyWithComplexSchema() {
        // OpenAPI spec with complex nested response body
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "components": {
                "schemas": {
                  "User": {
                    "type": "object",
                    "properties": {
                      "id": {
                        "type": "integer"
                      },
                      "profile": {
                        "$ref": "#/components/schemas/Profile"
                      }
                    }
                  },
                  "Profile": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      },
                      "age": {
                        "type": "integer"
                      }
                    }
                  }
                }
              },
              "paths": {
                "/user": {
                  "get": {
                    "summary": "Get user",
                    "responses": {
                      "200": {
                        "description": "Successful response",
                        "content": {
                          "application/json": {
                            "schema": {
                              "$ref": "#/components/schemas/User"
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

        // Set the OpenAPI spec
        testSourceSystem.openApiSpec = openApiSpec;
        testSourceSystem.persist();

        // Synchronize from spec
        openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);

        // Verify that endpoint was created with response body schema
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(1, endpoints.size(), "Should create one endpoint");

        SourceSystemEndpoint endpoint = endpoints.get(0);
        assertEquals("/user", endpoint.endpointPath, "Endpoint path should match");
        assertEquals("GET", endpoint.httpRequestType, "HTTP method should match");
        
        assertNotNull(endpoint.responseBodySchema, "Response body schema should be extracted");
        assertTrue(endpoint.responseBodySchema.contains("User"), "Response body schema should contain User reference");
        assertTrue(endpoint.responseBodySchema.contains("Profile"), "Response body schema should contain Profile reference");
    }

    @Test
    public void testExtractResponseBodyWithInvalidJson() {
        // Invalid OpenAPI spec
        String invalidOpenApiSpec = "invalid json";

        // Set the invalid OpenAPI spec
        testSourceSystem.openApiSpec = invalidOpenApiSpec;
        testSourceSystem.persist();

        // Synchronize from spec should not throw exception but handle gracefully
        assertDoesNotThrow(() -> {
            openApiSpecificationParserService.synchronizeFromSpec(testSourceSystem);
        }, "Should handle invalid OpenAPI spec gracefully");

        // Verify that no endpoints were created
        List<SourceSystemEndpoint> endpoints = SourceSystemEndpoint.findBySourceSystemId(testSourceSystem.id);
        assertEquals(0, endpoints.size(), "Should not create endpoints from invalid spec");
    }
} 