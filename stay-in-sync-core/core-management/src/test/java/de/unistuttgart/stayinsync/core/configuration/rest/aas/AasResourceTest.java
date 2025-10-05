package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
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

@QuarkusTest
public class AasResourceTest {

    @InjectMock
    AasTraversalClient traversal;

    @BeforeEach
    @Transactional
    void seed() {
        // Clean up in correct order to avoid foreign key constraints
        SourceSystem ss = new SourceSystem();
        ss.apiType = "AAS";
        ss.apiUrl = "http://aas.example";
        ss.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        ss.name = "test";
        ss.persist();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

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

    @Test
    void listSubmodels_live_ok() {
        String payload = "{\"result\":[{\"type\":\"ModelReference\",\"keys\":[{\"type\":\"Submodel\",\"value\":\"id\"}]}]}";
        HttpResponse<Buffer> httpResp = mockResp(200, payload);
        Mockito.when(traversal.listSubmodels(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(httpResp));
        
        // Mock getSubmodel to return success for the submodel reference
        HttpResponse<Buffer> getSubmodelResp = mockResp(200, "{\"idShort\":\"test-submodel\"}");
        Mockito.when(traversal.getSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(getSubmodelResp));

        given()
            .queryParam("source", "LIVE")
            .when().get("/api/config/source-system/{id}/aas/submodels", SourceSystem.<SourceSystem>findAll().firstResult().id)
            .then()
            .statusCode(200)
            .body("size()", equalTo(1));
    }

    private HttpResponse<Buffer> mockResp(int code, String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<Buffer> resp = (HttpResponse<Buffer>) Mockito.mock(HttpResponse.class);
        Mockito.when(resp.statusCode()).thenReturn(code);
        Mockito.when(resp.statusMessage()).thenReturn("OK");
        Mockito.when(resp.bodyAsString()).thenReturn(body);
        return resp;
    }
}



