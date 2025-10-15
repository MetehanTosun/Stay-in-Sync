package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Integration tests for AAS resource endpoints related to Source Systems.
 * Verifies AAS connection tests and submodel listing using mocked AAS traversal client responses.
 */
@QuarkusTest
public class AasResourceTest {

    @InjectMock
    AasTraversalClient traversal;

    /**
     * Prepares test data before each test case by creating and persisting a SourceSystem entity.
     * Enables RestAssured logging to assist in debugging failed validations.
     */
    @BeforeEach
    @Transactional
    void seed() {
        SourceSystem ss = new SourceSystem();
        ss.apiType = "AAS";
        ss.apiUrl = "http://aas.example";
        ss.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        ss.name = "test";
        ss.persist();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Tests successful AAS shell retrieval through the /aas/test endpoint.
     * Mocks a valid AAS response and verifies that the API returns HTTP 200 with the expected JSON structure.
     */
    @Test
    void testEndpoint_ok() {
        HttpResponse<Buffer> httpResp = mockResp(200, "{\"idShort\":\"shell\"}");
        Mockito.when(traversal.getShell(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(httpResp));

        given()
            .contentType("application/json")
            .body("{}")
            .when().post("/api/config/source-system/{id}/aas/test", SourceSystem.<SourceSystem>findAll().firstResult().id)
            .then()
            .statusCode(200)
            .body("idShort", equalTo("shell"));
    }

    /**
     * Tests successful retrieval of submodels from the AAS using the /aas/submodels endpoint.
     * Verifies that the API returns a list of submodels and responds with HTTP 200.
     */
    @Test
    void listSubmodels_live_ok() {
        String payload = "{\"result\":[{\"type\":\"ModelReference\",\"keys\":[{\"type\":\"Submodel\",\"value\":\"id\"}]}]}";
        HttpResponse<Buffer> httpResp = mockResp(200, payload);
        Mockito.when(traversal.listSubmodels(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(httpResp));
        
        HttpResponse<Buffer> getSubmodelResp = mockResp(200, "{\"idShort\":\"test-submodel\"}");
        Mockito.when(traversal.getSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(getSubmodelResp));

        given()
            .queryParam("source", "LIVE")
            .when().get("/api/config/source-system/{id}/aas/submodels", SourceSystem.<SourceSystem>findAll().firstResult().id)
            .then()
            .statusCode(200)
            .body("size()", equalTo(1));
    }

    /**
     * Utility method to create a mocked HTTP response for traversal client calls.
     *
     * @param code The HTTP status code to simulate.
     * @param body The response body as a JSON string.
     * @return A mocked {@link HttpResponse} containing the given code and body.
     */
    private HttpResponse<Buffer> mockResp(int code, String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<Buffer> resp = (HttpResponse<Buffer>) Mockito.mock(HttpResponse.class);
        Mockito.when(resp.statusCode()).thenReturn(code);
        Mockito.when(resp.statusMessage()).thenReturn("OK");
        Mockito.when(resp.bodyAsString()).thenReturn(body);
        return resp;
    }
}
