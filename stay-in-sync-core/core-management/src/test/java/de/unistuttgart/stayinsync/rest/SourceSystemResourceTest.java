package de.unistuttgart.stayinsync.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class SourceSystemResourceTest {

    /**
     * Test that getting all SourceSystems returns an empty list initially.
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
                    "id": %s, 
                    "name": "SensorAfterUpdate",
                    "description": "Description after update",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:2222"
                }
                """.formatted(id);

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
}