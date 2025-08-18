package de.unistuttgart.stayinsync.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.GetTypeDefinitionsResponseDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration.TypeLibraryDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TargetDtsBuilderGeneratorService;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationPatternType;
import io.quarkus.test.junit.QuarkusTest;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TargetDtsBuilderGeneratorServiceTest {

    @Inject
    TargetDtsBuilderGeneratorService service;

    private Transformation testTransformation;
    private TargetSystem testTargetSystem;
    private TargetSystemApiRequestConfiguration targetRequestConfiguration;

    private Long testTransformationId;

    @BeforeEach
    @Transactional
    void setUp() {
        Transformation.deleteAll();
        TargetSystemApiRequestConfigurationAction.deleteAll();
        TargetSystemApiRequestConfiguration.deleteAll();
        TargetSystemEndpoint.deleteAll();
        TargetSystem.deleteAll();
        SourceSystemEndpoint.deleteAll();

        testTargetSystem = new TargetSystem();
        testTargetSystem.name = "TestSystem";
        testTargetSystem.openApiSpec = getTestOpenApiSpec();
        testTargetSystem.persist();

        TargetSystemEndpoint checkEndpoint = new TargetSystemEndpoint();
        checkEndpoint.endpointPath = "/products/{id}";
        checkEndpoint.httpRequestType = "GET";
        checkEndpoint.syncSystem = testTargetSystem;
        checkEndpoint.persist();

        TargetSystemEndpoint createEndpoint = new TargetSystemEndpoint();
        createEndpoint.endpointPath = "/products";
        createEndpoint.httpRequestType = "POST";
        createEndpoint.syncSystem = testTargetSystem;
        createEndpoint.persist();

        TargetSystemEndpoint updateEndpoint = new TargetSystemEndpoint();
        updateEndpoint.endpointPath = "/products/{id}";
        updateEndpoint.httpRequestType = "PUT";
        updateEndpoint.syncSystem = testTargetSystem;
        updateEndpoint.persist();

        targetRequestConfiguration = new TargetSystemApiRequestConfiguration();
        targetRequestConfiguration.alias = "productSystem";
        targetRequestConfiguration.arcPatternType = TargetApiRequestConfigurationPatternType.OBJECT_UPSERT;
        targetRequestConfiguration.targetSystem = testTargetSystem;
        targetRequestConfiguration.actions = new ArrayList<>();
        targetRequestConfiguration.persist();

        TargetSystemApiRequestConfigurationAction checkAction = new TargetSystemApiRequestConfigurationAction();
        checkAction.actionRole = TargetApiRequestConfigurationActionRole.CHECK;
        checkAction.endpoint = checkEndpoint;
        checkAction.targetSystemApiRequestConfiguration = targetRequestConfiguration;
        checkAction.executionOrder = 1;
        checkAction.persist();

        TargetSystemApiRequestConfigurationAction createAction = new TargetSystemApiRequestConfigurationAction();
        createAction.actionRole = TargetApiRequestConfigurationActionRole.CREATE;
        createAction.endpoint = createEndpoint;
        createAction.targetSystemApiRequestConfiguration = targetRequestConfiguration;
        createAction.executionOrder = 2;
        createAction.persist();

        TargetSystemApiRequestConfigurationAction updateAction = new TargetSystemApiRequestConfigurationAction();
        updateAction.actionRole = TargetApiRequestConfigurationActionRole.UPDATE;
        updateAction.endpoint = updateEndpoint;
        updateAction.targetSystemApiRequestConfiguration = targetRequestConfiguration;
        updateAction.executionOrder = 3;
        updateAction.persist();

        targetRequestConfiguration.actions = List.of(checkAction, createAction, updateAction);

        testTransformation = new Transformation();
        testTransformation.name = "TestTransformation";
        testTransformation.targetSystemApiRequestConfigurations = Set.of(targetRequestConfiguration);
        testTransformation.persist();

        testTransformationId = testTransformation.id;
    }

    @Test
    @Transactional
    void testGenerateForTransformation() {
        GetTypeDefinitionsResponseDTO response = service.generateForTransformation(testTransformationId);

        assertNotNull(response);
        assertNotNull(response.libraries());
        assertEquals(5, response.libraries().size(), "Should contain base library, shared models, one arc library, one contract Library and one manifest");

        TypeLibraryDTO sharedModelsLibrary = response.libraries().stream()
                .filter(lib -> lib.filePath().equals("stayinsync/shared/models.d.ts"))
                .findFirst()
                .orElse(null);
        assertNotNull(sharedModelsLibrary);

        assertTrue(sharedModelsLibrary.content().contains("declare interface Product"), "Should contain Product interface");
        assertTrue(sharedModelsLibrary.content().contains("declare interface ProductUpdate"), "Should contain ProductUpdate interface");
        assertTrue(sharedModelsLibrary.content().contains("status: \"active\" | \"inactive\" | \"discontinued\""), "Should contain status enum");

        TypeLibraryDTO arcLibrary = response.libraries().stream()
                .filter(lib -> lib.filePath().equals("stayinsync/targets/arcs/productSystem.d.ts"))
                .findFirst()
                .orElse(null);
        assertNotNull(arcLibrary);

        assertTrue(arcLibrary.content().contains("declare class ProductSystem_Client"), "Should declare the client class");
        assertTrue(arcLibrary.content().contains("declare interface ProductSystem_UpsertBuilder"), "Should declare the main upsert builder");
        assertTrue(arcLibrary.content().contains("usingCheck(config: (builder: CheckBuilder_Initial) => void): this;"), "Should have usingCheck method");
        assertTrue(arcLibrary.content().contains("usingCreate(config: (builder: CreateBuilder_Initial) => void): this;"), "Should have usingCreate method");
        assertTrue(arcLibrary.content().contains("usingUpdate(config: (builder: UpdateBuilder_Initial) => void): this;"), "Should have usingUpdate method");
        assertTrue(arcLibrary.content().contains("withPathParamId(idProvider: (checkResponse: Product) => number): UpdateBuilder_WithPayload;"), "Should have type-safe idProvider");


        TypeLibraryDTO manifestLibrary = response.libraries().stream()
                .filter(lib -> lib.filePath().equals("stayinsync/targets/manifest.d.ts"))
                .findFirst()
                .orElse(null);
        assertNotNull(manifestLibrary);

        assertTrue(manifestLibrary.content().contains("declare const targets:"), "Should declare the targets namespace");
        assertTrue(manifestLibrary.content().contains("productSystem: ProductSystem_Client;"), "Should map the alias to the client");
    }

    @Test
    void testParseSpecification() {
        OpenAPI spec = service.parseSpecification(getTestOpenApiSpec());
        assertNotNull(spec);
        assertEquals("Product API", spec.getInfo().getTitle());
        assertEquals("1.0.0", spec.getInfo().getVersion());
        assertNotNull(spec.getPaths());
        assertTrue(spec.getPaths().containsKey("/products"));
        assertTrue(spec.getPaths().containsKey("/products/{id}"));
    }


    private String getTestOpenApiSpec() {
        return """
        {
          "openapi": "3.0.0",
          "info": {
            "title": "Product API",
            "version": "1.0.0"
          },
          "paths": {
            "/products": {
              "post": {
                "operationId": "createProduct",
                "parameters": [
                  {
                    "name": "category",
                    "in": "query",
                    "required": true,
                    "schema": {
                      "type": "string",
                      "enum": ["electronics", "clothing", "food"]
                    }
                  }
                ],
                "requestBody": {
                  "required": true,
                  "content": {
                    "application/json": {
                      "schema": {
                        "$ref": "#/components/schemas/Product"
                      }
                    }
                  }
                },
                "responses": {
                  "201": {
                    "description": "Product created"
                  }
                }
              }
            },
            "/products/{id}": {
              "get": {
                "operationId": "getProduct",
                "parameters": [
                  {
                    "name": "id",
                    "in": "path",
                    "required": true,
                    "schema": {
                      "type": "integer"
                    }
                  },
                  {
                    "name": "includeDetails",
                    "in": "query",
                    "required": false,
                    "schema": {
                      "type": "boolean"
                    }
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Product found",
                    "content": {
                      "application/json": {
                        "schema": {
                          "$ref": "#/components/schemas/Product"
                        }
                      }
                    }
                  }
                }
              },
              "put": {
                "operationId": "updateProduct",
                "parameters": [
                  {
                    "name": "id",
                    "in": "path",
                    "required": true,
                    "schema": {
                      "type": "integer"
                    }
                  }
                ],
                "requestBody": {
                  "required": true,
                  "content": {
                    "application/json": {
                      "schema": {
                        "$ref": "#/components/schemas/ProductUpdate"
                      }
                    }
                  }
                },
                "responses": {
                  "200": {
                    "description": "Product updated"
                  }
                }
              }
            }
          },
          "components": {
            "schemas": {
              "Product": {
                "type": "object",
                "properties": {
                  "id": {
                    "type": "integer"
                  },
                  "name": {
                    "type": "string"
                  },
                  "price": {
                    "type": "number"
                  },
                  "description": {
                    "type": "string"
                  },
                  "status": {
                    "type": "string",
                    "enum": ["active", "inactive", "discontinued"]
                  },
                  "tags": {
                    "type": "array",
                    "items": {
                      "type": "string"
                    }
                  },
                  "metadata": {
                    "type": "object",
                    "properties": {
                      "createdBy": {
                        "type": "string"
                      },
                      "createdAt": {
                        "type": "string",
                        "format": "date-time"
                      }
                    }
                  }
                },
                "required": ["name", "price", "status"]
              },
              "ProductUpdate": {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string"
                  },
                  "price": {
                    "type": "number"
                  },
                  "status": {
                    "type": "string",
                    "enum": ["active", "inactive", "discontinued"]
                  },
                  "description": {
                    "type": "string"
                  }
                }
              }
            }
          }
        }
        """;
    }
}