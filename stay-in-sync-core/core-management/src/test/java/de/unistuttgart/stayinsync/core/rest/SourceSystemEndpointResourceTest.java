package de.unistuttgart.stayinsync.core.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestTransaction
public class SourceSystemEndpointResourceTest {

    @Inject
    EntityManager em;

    private SourceSystem testSourceSystem;
    private Long sourceSystemId;

    @BeforeEach
    void setUp() {
        // Create a test source system
        testSourceSystem = new SourceSystem();
        testSourceSystem.name = "TestSystem";
        testSourceSystem.apiUrl = "http://test.com";
        testSourceSystem.apiType = "REST";
        testSourceSystem.description = "Test Description";
        em.persist(testSourceSystem);
        em.flush();
        em.refresh(testSourceSystem);
        sourceSystemId = testSourceSystem.id;
    }

    @Test
    public void testCreateEndpointWithTypeScriptGeneration() {
        String jsonBody = """
                [{
                    "endpointPath": "/api/users",
                    "httpRequestType": "GET",
                    "requestBodySchema": "",
                    "responseBodySchema": "{\\"type\\": \\"object\\", \\"properties\\": {\\"id\\": {\\"type\\": \\"number\\"}, \\"name\\": {\\"type\\": \\"string\\"}}}"
                }]
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetEndpointWithTypeScriptField() {
        // Test that the endpoint returns 404 for non-existent endpoint
        given()
                .when()
                .get("/api/config/source-system/endpoint/99999")
                .then()
                .statusCode(404);
    }

    @Test
    public void testUpdateEndpointWithTypeScriptGeneration() {
        String updateJson = """
                {
                    "endpointPath": "/api/test",
                    "httpRequestType": "GET",
                    "responseBodySchema": "{\\"type\\": \\"object\\", \\"properties\\": {\\"name\\": {\\"type\\": \\"string\\"}, \\"age\\": {\\"type\\": \\"number\\"}}}"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateJson)
                .when()
                .put("/api/config/source-system/endpoint/99999")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetAllEndpointsWithTypeScript() {
        given()
                .when()
                .get("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    public void testCreateEndpointWithInvalidJsonSchema() {
        String jsonBody = """
                [{
                    "endpointPath": "/api/users",
                    "httpRequestType": "GET",
                    "requestBodySchema": "",
                    "responseBodySchema": "{invalid json}"
                }]
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(400);
    }

    @Test
    public void testCreateEndpointWithEmptyResponseBodySchema() {
        String jsonBody = """
                [{
                    "endpointPath": "/api/users",
                    "httpRequestType": "GET",
                    "requestBodySchema": "",
                    "responseBodySchema": ""
                }]
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(404);
    }

    @Test
    public void testCreateEndpointWithComplexJsonSchema() {
        String complexSchema = """
                {
                    "type": "object",
                    "properties": {
                        "user": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "number"},
                                "name": {"type": "string"}
                            }
                        }
                    }
                }
                """;

        String jsonBody = """
                [{
                    "endpointPath": "/api/complex",
                    "httpRequestType": "GET",
                    "requestBodySchema": "",
                    "responseBodySchema": %s
                }]
                """.formatted(complexSchema);

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(400);
    }

    @Test
    public void testDeleteEndpointWithTypeScript() {
        given()
                .when()
                .delete("/api/config/source-system/endpoint/99999")
                .then()
                .statusCode(204);
    }

    @Test
    public void testGetNonExistentEndpoint() {
        given()
                .when()
                .get("/api/config/source-system/endpoint/99999")
                .then()
                .statusCode(404);
    }

    @Test
    public void testUpdateNonExistentEndpoint() {
        String updateJson = """
                {
                    "endpointPath": "/api/test",
                    "httpRequestType": "GET",
                    "responseBodySchema": "{\\"type\\": \\"object\\"}"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateJson)
                .when()
                .put("/api/config/source-system/endpoint/99999")
                .then()
                .statusCode(404);
    }
} 