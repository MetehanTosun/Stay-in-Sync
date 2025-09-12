package de.unistuttgart.stayinsync.core.resource;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TargetSystemService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class TargetSystemEndpointResourceTest {

    @Inject
    EntityManager em;

    @Inject
    TargetSystemService targetSystemService;

    Long targetSystemId;

    @BeforeEach
    @Transactional
    public void setup() {
        try {
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE TargetSystemEndpoint").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE TargetSystem").executeUpdate();
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
        } catch (Exception ignored) { }

        var ts = targetSystemService.createTargetSystem(new TargetSystemDTO(null, "TS-R", "http://ts", null, "REST", null, java.util.Set.of()));
        targetSystemId = ts.id();
    }

    @Test
    public void crud_flow() {
        // create list of endpoints
        List<Map<String, String>> body = List.of(
                Map.of("endpointPath", "/u", "httpRequestType", "GET"),
                Map.of("endpointPath", "/p", "httpRequestType", "POST")
        );

        var createResp = given()
                .port(8081)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/target-systems/" + targetSystemId + "/endpoint")
                .then()
                .statusCode(201)
                .body("size()", is(2))
                .extract().response();

        Long id = createResp.jsonPath().getLong("[0].id");

        // list
        given()
                .port(8081)
                .when()
                .get("/api/target-systems/" + targetSystemId + "/endpoint")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("httpRequestType", hasItems("GET", "POST"));

        // get by id
        given()
                .port(8081)
                .when()
                .get("/api/target-systems/endpoint/" + id)
                .then()
                .statusCode(200)
                .body("id", is(id.intValue()))
                .body("endpointPath", is("/u"));

        // update (replace)
        Map<String, String> updateBody = Map.of("endpointPath", "/u2", "httpRequestType", "PUT");
        given()
                .port(8081)
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/target-systems/endpoint/" + id)
                .then()
                .statusCode(anyOf(is(204), is(200)));

        // verify updated
        given()
                .port(8081)
                .when()
                .get("/api/target-systems/endpoint/" + id)
                .then()
                .statusCode(200)
                .body("httpRequestType", is("PUT"))
                .body("endpointPath", is("/u2"));

        // delete
        given()
                .port(8081)
                .when()
                .delete("/api/target-systems/endpoint/" + id)
                .then()
                .statusCode(anyOf(is(204), is(200)));

        // verify deleted
        given()
                .port(8081)
                .when()
                .get("/api/target-systems/endpoint/" + id)
                .then()
                .statusCode(404);
    }
}


