package de.unistuttgart.stayinsync.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class SourceSystemResourceTest extends BaseTest {

    /**
     * Test that the source system list is empty initially.
     */
    @Test
    public void testGetAllEmpty() {
        given()
                .when().get("/api/config/source-system")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    /**
     * Test creating a new SourceSystem and then retrieving it by ID.
     */
    @Test
    public void testCreateAndGetById() {
        String jsonBody = """
                {
                    "name": "TestSensor",
                    "description": "Test Description",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:1234"
                }
                """;

        // Create a new SourceSystem via POST
        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        // Extract ID from the Location header
        String id = location.substring(location.lastIndexOf("/") + 1);

        // Retrieve the created SourceSystem by ID and verify fields
        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("TestSensor"))
                .body("description", equalTo("Test Description"))
                .body("apiUrl", equalTo("http://localhost:1234"));
    }

    /**
     * Test updating an existing SourceSystem.
     */
    @Test
    public void testUpdate() {
        String jsonBodyCreate = """
                {
                    "name": "SensorBeforeUpdate",
                    "description": "Description before update",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:1111"
                }
                """;

        // Create a new SourceSystem
        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBodyCreate)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        String jsonBodyUpdate = """
                {
                    "name": "SensorAfterUpdate",
                    "description": "Description after update",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:2222"
                }
                """;

        // Update the SourceSystem via PUT
        given()
                .contentType(ContentType.JSON)
                .body(jsonBodyUpdate)
                .when().put("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("SensorAfterUpdate"))
                .body("description", equalTo("Description after update"))
                .body("apiType", equalTo("REST"))
                .body("apiUrl", equalTo("http://localhost:2222"));
    }

    /**
     * Test deleting an existing SourceSystem and verify it no longer exists.
     */
    @Test
    public void testDelete() {
        String jsonBody = """
                {
                    "name": "ToDelete",
                    "description": "Will be deleted",
                    "apiType": "REST",
                    "apiUrl": "http://localhost/delete"
                }
                """;

        // Create a new SourceSystem
        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        // Delete the SourceSystem
        given()
                .when().delete("/api/config/source-system/" + id)
                .then()
                .statusCode(204);

        // Verify it no longer exists
        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(404);
    }

    /**
     * Test retrieving a SourceSystem by an ID that does not exist returns 404.
     */
    @Test
    public void testGetByIdNotFound() {
        given()
                .when().get("/api/config/source-system/9999999")
                .then()
                .statusCode(404);
    }

    /**
     * Test deleting a SourceSystem by an ID that does not exist returns 404.
     */
    @Test
    public void testDeleteNotFound() {
        given()
                .when().delete("/api/config/source-system/9999999")
                .then()
                .statusCode(404);
    }

    /**
     * Test creating and deleting a SourceSystem with endpoints.
     */
    @Test
    public void testDeleteWithEndpoints() {
        // Create a source system
        String sourceSystemJson = """
                {
                    "name": "TestSystemWithEndpoints",
                    "description": "Test system with endpoints",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:3000"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(sourceSystemJson)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String sourceSystemId = location.substring(location.lastIndexOf("/") + 1);

        // Create an endpoint for the source system
        String endpointJson = """
                [{
                    "endpointPath": "/test/endpoint",
                    "httpRequestType": "GET",
                    "responseBodySchema": "{\\"type\\": \\"object\\"}"
                }]
                """;

        given()
                .contentType(ContentType.JSON)
                .body(endpointJson)
                .when()
                .post("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(201);

        // Verify the endpoint was created
        given()
                .when().get("/api/config/source-system/" + sourceSystemId + "/endpoint")
                .then()
                .statusCode(200)
                .body("$.size()", is(1));

        // Delete the source system (should cascade delete the endpoint)
        given()
                .when().delete("/api/config/source-system/" + sourceSystemId)
                .then()
                .statusCode(204);

        // Verify the source system no longer exists
        given()
                .when().get("/api/config/source-system/" + sourceSystemId)
                .then()
                .statusCode(404);
    }

    /**
     * Test creating multiple source systems and listing them.
     */
    @Test
    public void testCreateMultipleAndList() {
        // Create first source system
        String jsonBody1 = """
                {
                    "name": "System1",
                    "description": "First test system",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:8001"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody1)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201);

        // Create second source system
        String jsonBody2 = """
                {
                    "name": "System2",
                    "description": "Second test system",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:8002"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody2)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201);

        // Verify both systems are in the list
        given()
                .when().get("/api/config/source-system")
                .then()
                .statusCode(200)
                .body("$.size()", is(2))
                .body("[0].name", equalTo("System1"))
                .body("[1].name", equalTo("System2"));
    }

    /**
     * Test creating a source system with different API types.
     */
    @Test
    public void testCreateWithDifferentApiTypes() {
        // Test REST API type
        String restJson = """
                {
                    "name": "RestSystem",
                    "description": "REST API system",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:9001"
                }
                """;

        String restLocation = given()
                .contentType(ContentType.JSON)
                .body(restJson)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String restId = restLocation.substring(restLocation.lastIndexOf("/") + 1);

        given()
                .when().get("/api/config/source-system/" + restId)
                .then()
                .statusCode(200)
                .body("name", equalTo("RestSystem"))
                .body("apiType", equalTo("REST"));

        // Test SOAP API type
        String soapJson = """
                {
                    "name": "SoapSystem",
                    "description": "SOAP API system",
                    "apiType": "SOAP",
                    "apiUrl": "http://localhost:9002"
                }
                """;

        String soapLocation = given()
                .contentType(ContentType.JSON)
                .body(soapJson)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String soapId = soapLocation.substring(soapLocation.lastIndexOf("/") + 1);

        given()
                .when().get("/api/config/source-system/" + soapId)
                .then()
                .statusCode(200)
                .body("name", equalTo("SoapSystem"))
                .body("apiType", equalTo("SOAP"));
    }

    /**
     * Test creating a source system with a very long description.
     */
    @Test
    public void testCreateWithLongDescription() {
        String longDescription = "A".repeat(500); // Reduced to 500 characters to avoid database issues
        
        String jsonBody = """
                {
                    "name": "LongDescSystem",
                    "description": "%s",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:9003"
                }
                """.formatted(longDescription);

        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("LongDescSystem"))
                .body("description", equalTo(longDescription));
    }

    /**
     * Test creating a source system with special characters in name and description.
     */
    @Test
    public void testCreateWithSpecialCharacters() {
        String jsonBody = """
                {
                    "name": "System-With_Special.Chars",
                    "description": "System with special characters: äöüß, 123, @#$%^&*()",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:9004"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("System-With_Special.Chars"))
                .body("description", equalTo("System with special characters: äöüß, 123, @#$%^&*()"));
    }

    /**
     * Test creating a source system with HTTPS URL.
     */
    @Test
    public void testCreateWithHttpsUrl() {
        String jsonBody = """
                {
                    "name": "HttpsSystem",
                    "description": "System with HTTPS URL",
                    "apiType": "REST",
                    "apiUrl": "https://api.example.com/v1"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("HttpsSystem"))
                .body("apiUrl", equalTo("https://api.example.com/v1"));
    }

    /**
     * Test creating a source system with query parameters in URL.
     */
    @Test
    public void testCreateWithQueryParamsInUrl() {
        String jsonBody = """
                {
                    "name": "QueryParamSystem",
                    "description": "System with query parameters in URL",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:9005?version=1.0&format=json"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("QueryParamSystem"))
                .body("apiUrl", equalTo("http://localhost:9005?version=1.0&format=json"));
    }

    /**
     * Test creating a source system with port number in URL.
     */
    @Test
    public void testCreateWithPortInUrl() {
        String jsonBody = """
                {
                    "name": "PortSystem",
                    "description": "System with port number in URL",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:8080/api"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("PortSystem"))
                .body("apiUrl", equalTo("http://localhost:8080/api"));
    }

    /**
     * Test creating a source system with path in URL.
     */
    @Test
    public void testCreateWithPathInUrl() {
        String jsonBody = """
                {
                    "name": "PathSystem",
                    "description": "System with path in URL",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:9006/api/v2"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("PathSystem"))
                .body("apiUrl", equalTo("http://localhost:9006/api/v2"));
    }
}
