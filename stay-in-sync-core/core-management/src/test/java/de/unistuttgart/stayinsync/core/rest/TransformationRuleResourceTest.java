package de.unistuttgart.stayinsync.core.rest;


import de.unistuttgart.graphengine.dto.transformationrule.TransformationRulePayloadDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the {@code TransformationRuleResource} REST endpoints.
 * <p>
 * These tests are ordered to simulate the complete lifecycle of a transformation rule:
 * Create, Update (with various graph states), and Delete.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransformationRuleResourceTest {

    /**
     * Stores the ID of the rule created in the first test to be reused in subsequent tests.
     */
    private static int createdRuleId;

    /**
     * Tests the creation of a new transformation rule.
     * <p>
     * It sends a POST request with a valid payload and expects an HTTP 201 (Created) status.
     * The response body is validated to ensure it contains the correct data for the newly created rule.
     * The ID of the created rule is extracted and stored for later tests.
     */
    @Test
    @Order(1)
    void testCreateRuleEndpoint_ShouldReturnCreated() {
        TransformationRulePayloadDTO payload = new TransformationRulePayloadDTO();
        payload.setName("My API Test Rule");
        payload.setDescription("A test rule created via REST Assured.");

        createdRuleId = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/config/transformation-rule")
                .then()
                .statusCode(201) // 201 Created
                .body("name", equalTo("My API Test Rule"))
                .body("graphStatus", equalTo("FINALIZED")) // The default graph should be valid
                .extract().path("id");
    }

    /**
     * Tests updating the graph with a malformed JSON payload.
     * <p>
     * The server is expected to reject the request due to the syntax error in the JSON.
     * This test asserts that the server correctly returns an HTTP 400 (Bad Request) status.
     */
    @Test
    @Order(2)
    void testUpdateGraphEndpoint_WithInvalidJson_ShouldReturnBadRequest() {
        // This JSON is malformed (an extra trailing comma)
        String malformedJson = """
        {
          "nodes": [],
          "edges": [],
        }
        """;

        given()
                .contentType(ContentType.JSON)
                .body(malformedJson)
                .when()
                .put("/api/config/transformation-rule/" + createdRuleId + "/graph")
                .then()
                .statusCode(400); // 400 Bad Request
    }

    /**
     * Tests updating the graph with a payload that is syntactically correct but logically invalid
     * (e.g., contains cycles, or disconnected nodes).
     * <p>
     * The server should accept the request with an HTTP 200 (OK) status but return a non-empty
     * list of validation errors. A subsequent GET request verifies that the rule's status
     * has been correctly updated to DRAFT.
     */
    @Test
    @Order(3)
    void testUpdateGraphEndpoint_WithLogicallyInvalidGraph_ShouldReturnOkWithErrors() {
        String logicallyInvalidGraphJson =
                """
                {
                  "nodes": [
                    {
                      "id": "0",
                      "type": "CONFIG",
                      "point": { "x": 50, "y": 50 },
                      "data": { "name": "Config", "nodeType": "CONFIG" }
                    },
                    {
                      "id": "1",
                      "type": "FINAL",
                      "point": { "x": 250, "y": 50 },
                      "data": { "name": "Final Node", "nodeType": "FINAL" }
                    }
                  ],
                  "edges": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(logicallyInvalidGraphJson)
                .when()
                .put("/api/config/transformation-rule/" + createdRuleId + "/graph")
                .then()
                .statusCode(200) // 200 OK
                .body("validationErrors", not(empty()));

        given()
                .when()
                .get("/api/config/transformation-rule/" + createdRuleId)
                .then()
                .statusCode(200)
                .body("graphStatus", equalTo("DRAFT"));
    }

    /**
     * Tests updating the graph with a logically valid payload.
     * <p>
     * This test expects an HTTP 200 (OK) status and asserts that the list of validation errors
     * in the response is null or empty. A subsequent GET request verifies that the rule's status
     * has been updated back to FINALIZED.
     */
    @Test
    @Order(4)
    void testUpdateGraphEndpoint_WithValidGraph_ShouldReturnOk() {
        String validGraphJson =
                """
                {
                  "nodes": [
                    {
                      "id": "0",
                      "type": "CONFIG",
                      "point": { "x": 50, "y": 50 },
                      "data": { "name": "Config", "nodeType": "CONFIG" }
                    },
                    {
                      "id": "1",
                      "type": "FINAL",
                      "point": { "x": 250, "y": 50 },
                      "data": { "name": "Final Node", "nodeType": "FINAL" }
                    }
                  ],
                  "edges": [
                    { "id": "0->1", "source": "0", "target": "1", "targetHandle": "input-0" }
                  ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(validGraphJson)
                .when()
                .put("/api/config/transformation-rule/" + createdRuleId + "/graph")
                .then()
                .statusCode(200)
                .body("validationErrors", is(nullValue())); // Or is(empty()), depending on implementation

        // Additional check: The rule's status should now be FINALIZED again.
        given()
                .when()
                .get("/api/config/transformation-rule/" + createdRuleId)
                .then()
                .statusCode(200)
                .body("graphStatus", equalTo("FINALIZED"));
    }

    /**
     * Tests the deletion of the previously created transformation rule.
     * <p>
     * It sends a DELETE request and expects an HTTP 204 (No Content) status.
     * To confirm the deletion, a subsequent GET request is made for the same ID,
     * which is expected to result in an HTTP 404 (Not Found) status.
     */
    @Test
    @Order(5)
    void testDeleteRuleEndpoint_ShouldReturnNoContent() {
        given()
                .when()
                .delete("/api/config/transformation-rule/" + createdRuleId)
                .then()
                .statusCode(204); // 204 No Content

        // Verify that the rule was actually deleted.
        given()
                .when()
                .get("/api/config/transformation-rule/" + createdRuleId)
                .then()
                .statusCode(404); // 404 Not Found
    }
}