package de.unistuttgart.stayinsync.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
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
                .statusCode(201);
    }

    @Test
    public void testGetEndpointWithTypeScriptField() {
        // Create an endpoint with TypeScript
        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/api/test";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}}}";
        endpoint.responseDts = "interface ResponseBody { id: number; }";
        endpoint.syncSystem = testSourceSystem; // Use syncSystem for foreign key
        em.persist(endpoint);
        em.flush();

        given()
                .when()
                .get("/api/config/source-system/endpoint/" + endpoint.id)
                .then()
                .statusCode(200)
                .body("endpointPath", equalTo("/api/test"))
                .body("httpRequestType", equalTo("GET"))
                .body("responseBodySchema", equalTo("{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}}}"))
                .body("responseDts", equalTo("interface ResponseBody { id: number; }"));
    }

    @Test
    public void testUpdateEndpointWithTypeScriptGeneration() {
        // Create an endpoint
        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/api/test";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}}}";
        endpoint.syncSystem = testSourceSystem; // Use syncSystem for foreign key
        em.persist(endpoint);
        em.flush();

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
                .put("/api/config/source-system/endpoint/" + endpoint.id)
                .then()
                .statusCode(200)
                .body("responseBodySchema", equalTo("{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, \"age\": {\"type\": \"number\"}}}"));
    }

    @Test
    public void testGetAllEndpointsWithTypeScript() {
        // Create multiple endpoints with TypeScript
        SourceSystemEndpoint endpoint1 = new SourceSystemEndpoint();
        endpoint1.endpointPath = "/api/users";
        endpoint1.httpRequestType = "GET";
        endpoint1.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}}}";
        endpoint1.responseDts = "interface ResponseBody { id: number; }";
        endpoint1.syncSystem = testSourceSystem; // Use syncSystem for foreign key
        em.persist(endpoint1);

        SourceSystemEndpoint endpoint2 = new SourceSystemEndpoint();
        endpoint2.endpointPath = "/api/posts";
        endpoint2.httpRequestType = "POST";
        endpoint2.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"title\": {\"type\": \"string\"}}}";
        endpoint2.responseDts = "interface ResponseBody { title: string; }";
        endpoint2.syncSystem = testSourceSystem; // Use syncSystem for foreign key
        em.persist(endpoint2);

        em.flush();

        given()
                .when()
                .get("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(200)
                .body("$.size()", is(2))
                .body("[0].responseDts", notNullValue())
                .body("[1].responseDts", notNullValue());
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
                .statusCode(201);
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
                    "responseBodySchema": "%s"
                }]
                """.formatted(complexSchema.replace("\"", "\\\""));

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(201);
    }

    @Test
    public void testDeleteEndpointWithTypeScript() {
        // Create an endpoint with TypeScript
        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/api/test";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}}}";
        endpoint.responseDts = "interface ResponseBody { id: number; }";
        endpoint.syncSystem = testSourceSystem; // Use syncSystem for foreign key
        em.persist(endpoint);
        em.flush();

        given()
                .when()
                .delete("/api/config/source-system/endpoint/" + endpoint.id)
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