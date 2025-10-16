package de.unistuttgart.stayinsync.core.configuration.rest.aas.source;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder;
import de.unistuttgart.stayinsync.core.configuration.service.aas.SourceSystemAasService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Integration tests for {@link de.unistuttgart.stayinsync.core.configuration.rest.aas.source.SourceAasValueController}.
 * Tests focus on retrieving and updating AAS element values from Source Systems through the REST API.
 * Scenarios include success cases, not found errors, invalid payloads, and non-existent Source Systems.
 */
@QuarkusTest
public class SourceAasValueControllerTest {

    @InjectMock
    AasTraversalClient traversal;

    @InjectMock
    SourceSystemAasService aasService;

    @InjectMock
    HttpHeaderBuilder headerBuilder;

    /**
     * Enables detailed RestAssured logging in case of validation failures
     * to assist in debugging failed REST assertions.
     */
    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Utility helper to create a mock HTTP response object with a specified status and body content.
     *
     * @param statusCode HTTP status code for the mock response.
     * @param body Response body string to include.
     * @return A mocked {@link HttpResponse} with the given status and body.
     */
    private HttpResponse<Buffer> mockResponse(int statusCode, String body) {
        HttpResponse<Buffer> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(statusCode);
        Mockito.when(response.statusMessage()).thenReturn("OK");
        Mockito.when(response.bodyAsString()).thenReturn(body);
        return response;
    }

    /**
     * Tests successful retrieval of an AAS element value from a Source System.
     * Verifies that the endpoint returns HTTP 200 and the expected JSON response body.
     */
    @Test
    void testGetElementValue_Success() {
        Long sourceSystemId = 1L;
        String smId = "test-submodel";
        String path = "test-element";
        String expectedValue = "{\"value\":\"test-value\"}";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = sourceSystemId;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(200, expectedValue);
        
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.getElement(anyString(), anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));

        given()
            .when()
            .get("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}/elements/{path}/value",
                 sourceSystemId, smId, path)
            .then()
            .statusCode(200)
            .body(equalTo(expectedValue));
    }

    /**
     * Tests behavior when attempting to retrieve a non-existent AAS element.
     * Expects the endpoint to return HTTP 404 (Not Found).
     */
    @Test
    void testGetElementValue_NotFound() {
        Long sourceSystemId = 1L;
        String smId = "test-submodel";
        String path = "non-existent-element";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = sourceSystemId;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(404, "Element not found");
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.getElement(anyString(), anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));
        Mockito.doThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException(jakarta.ws.rs.core.Response.Status.NOT_FOUND, "OK", "Element not found"))
                .when(aasService).throwHttpError(404, "OK", "Element not found");

        given()
            .when()
            .get("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}/elements/{path}/value",
                 sourceSystemId, smId, path)
            .then()
            .statusCode(404);
    }

    /**
     * Tests successful update of an AAS element value in a Source System.
     * Verifies that the endpoint returns HTTP 204 (No Content) on success.
     */
    @Test
    void testPatchElementValue_Success() {
        Long sourceSystemId = 1L;
        String smId = "test-submodel";
        String path = "test-element";
        String requestBody = "{\"value\":\"new-value\"}";
        String responseBody = "{\"status\":\"updated\"}";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = sourceSystemId;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(204, responseBody);
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.patchElementValue(anyString(), anyString(), anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));

        given()
            .contentType("application/json")
            .body(requestBody)
            .when()
            .patch("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}/elements/{path}/value",
                   sourceSystemId, smId, path)
            .then()
            .statusCode(204);
    }

    /**
     * Tests behavior when an invalid JSON payload is sent during a PATCH request.
     * Expects the endpoint to return HTTP 400 (Bad Request).
     */
    @Test
    void testPatchElementValue_BadRequest() {
        Long sourceSystemId = 1L;
        String smId = "test-submodel";
        String path = "test-element";
        String requestBody = "invalid-json";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = sourceSystemId;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(400, "Bad Request");
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.patchElementValue(anyString(), anyString(), anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));
        Mockito.doThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException(jakarta.ws.rs.core.Response.Status.BAD_REQUEST, "OK", "Bad Request"))
                .when(aasService).throwHttpError(400, "OK", "Bad Request");

        given()
            .contentType("application/json")
            .body(requestBody)
            .when()
            .patch("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}/elements/{path}/value",
                   sourceSystemId, smId, path)
            .then()
            .statusCode(400);
    }

    /**
     * Tests behavior when attempting to access an AAS element for a Source System that does not exist.
     * Expects the endpoint to return HTTP 404 (Not Found).
     */
    @Test
    void testGetElementValue_NonExistentSourceSystem() {
        Mockito.when(aasService.validateAasSource(Mockito.isNull()))
               .thenThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException(
                   jakarta.ws.rs.core.Response.Status.NOT_FOUND, "Source system not found", "Source system is null"));
        
        jakarta.ws.rs.core.Response errorResponse = jakarta.ws.rs.core.Response.status(404)
                .entity("Source system not found")
                .build();
        Mockito.doThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException(jakarta.ws.rs.core.Response.Status.NOT_FOUND, "Source system not found", "Source system is null"))
                .when(aasService).throwHttpError(404, "Source system not found", "Source system is null");

        given()
            .when()
            .get("/api/config/source-system/99999/aas/submodels/test/elements/test/value")
            .then()
            .statusCode(404);
    }
}
