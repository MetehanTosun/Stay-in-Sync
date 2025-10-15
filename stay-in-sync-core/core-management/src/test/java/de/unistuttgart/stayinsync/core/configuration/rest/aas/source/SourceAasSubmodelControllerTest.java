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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Integration tests for {@link de.unistuttgart.stayinsync.core.configuration.rest.aas.source.SourceAasSubmodelController}.
 * Verifies the REST endpoints for creating, updating, and deleting AAS submodels in Source Systems.
 * Uses mocked dependencies for AasTraversalClient, SourceSystemAasService, and HttpHeaderBuilder.
 */
@QuarkusTest
public class SourceAasSubmodelControllerTest {

    @InjectMock
    AasTraversalClient traversal;

    @InjectMock
    SourceSystemAasService aasService;

    @InjectMock
    HttpHeaderBuilder headerBuilder;

    /**
     * Configures RestAssured to log requests and responses when a validation fails.
     * Ensures test output visibility in case of assertion mismatches.
     */
    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Utility method to create a mock HttpResponse with the given status code and body.
     *
     * @param statusCode The HTTP status code to mock.
     * @param body The response body content.
     * @return A mocked HttpResponse object with the specified values.
     */
    private HttpResponse<Buffer> mockResponse(int statusCode, String body) {
        HttpResponse<Buffer> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(statusCode);
        Mockito.when(response.statusMessage()).thenReturn("OK");
        Mockito.when(response.bodyAsString()).thenReturn(body);
        return response;
    }

    /**
     * Tests successful creation of a submodel in a Source System.
     * Verifies that a 201 status code and correct response body are returned when the request succeeds.
     */
    @Test
    void testCreateSubmodel_Success() {
        Long sourceSystemId = 1L;
        String submodelJson = "{\"id\":\"test-submodel\",\"idShort\":\"TestSubmodel\",\"modelType\":\"Submodel\"}";
        String responseBody = "{\"id\":\"test-submodel\",\"idShort\":\"TestSubmodel\",\"modelType\":\"Submodel\"}";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = sourceSystemId;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> createResponse = mockResponse(201, responseBody);
        HttpResponse<Buffer> refResponse = mockResponse(200, "{\"status\":\"added\"}");
        
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.createSubmodel(anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(createResponse));
        Mockito.when(traversal.addSubmodelReferenceToShell(anyString(), anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(refResponse));

        given()
            .contentType("application/json")
            .body(submodelJson)
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/submodels", sourceSystemId)
            .then()
            .statusCode(201)
            .body(equalTo(responseBody));
    }

    /**
     * Tests submodel creation with invalid JSON input.
     * Expects a 400 Bad Request response when the payload is malformed.
     */
    @Test
    void testCreateSubmodel_BadRequest() {
        Long sourceSystemId = 1L;
        String invalidJson = "invalid-json";
        
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
        Mockito.when(traversal.createSubmodel(anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));
        Mockito.doThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException(jakarta.ws.rs.core.Response.Status.BAD_REQUEST, "OK", "Bad Request"))
                .when(aasService).throwHttpError(400, "OK", "Bad Request");

        given()
            .contentType("application/json")
            .body(invalidJson)
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/submodels", sourceSystemId)
            .then()
            .statusCode(400);
    }

    /**
     * Tests successful update of an existing submodel.
     * Verifies that a 200 OK status and updated response body are returned.
     */
    @Test
    void testUpdateSubmodel_Success() {
        String smId = "test-submodel";
        String submodelJson = "{\"id\":\"test-submodel\",\"idShort\":\"UpdatedSubmodel\",\"modelType\":\"Submodel\"}";
        String responseBody = "{\"id\":\"test-submodel\",\"idShort\":\"UpdatedSubmodel\",\"modelType\":\"Submodel\"}";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = 1L;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(200, responseBody);
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.putSubmodel(anyString(), anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));

        given()
            .contentType("application/json")
            .body(submodelJson)
            .when()
            .put("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}", 1L, smId)
            .then()
            .statusCode(200)
            .body(equalTo(responseBody));
    }

    /**
     * Tests successful deletion of a submodel from a Source System.
     * Verifies that a 200 status code is returned after deletion.
     */
    @Test
    void testDeleteSubmodel_Success() {
        String smId = "test-submodel";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = 1L;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(204, "");
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.deleteSubmodel(anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));

        given()
            .when()
            .delete("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}", 1L, smId)
            .then()
            .statusCode(200);
    }

    /**
     * Tests deletion of a non-existent submodel.
     * Expects a 404 Not Found response when the target submodel does not exist.
     */
    @Test
    void testDeleteSubmodel_NotFound() {
        String smId = "non-existent-submodel";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = 1L;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(404, "Submodel not found");
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.deleteSubmodel(anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));
        Mockito.doThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException(jakarta.ws.rs.core.Response.Status.NOT_FOUND, "OK", "Submodel not found"))
                .when(aasService).throwHttpError(404, "OK", "Submodel not found");

        given()
            .when()
            .delete("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}", 1L, smId)
            .then()
            .statusCode(404);
    }

    /**
     * Tests creation of a submodel for a Source System that does not exist.
     * Expects a 404 Not Found response indicating that the Source System could not be found.
     */
    @Test
    void testCreateSubmodel_NonExistentSourceSystem() {
        String submodelJson = "{\"id\":\"test-submodel\",\"idShort\":\"TestSubmodel\",\"modelType\":\"Submodel\"}";
        
        Mockito.when(aasService.validateAasSource(Mockito.isNull()))
                .thenThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException(
                        jakarta.ws.rs.core.Response.Status.NOT_FOUND, "Source system not found", "Source system is null"));
        
        jakarta.ws.rs.core.Response errorResponse = jakarta.ws.rs.core.Response.status(404)
                .entity("Source system not found")
                .build();
        Mockito.doThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException(jakarta.ws.rs.core.Response.Status.NOT_FOUND, "Source system not found", "Source system is null"))
                .when(aasService).throwHttpError(404, "Source system not found", "Source system is null");

        given()
            .contentType("application/json")
            .body(submodelJson)
            .when()
            .post("/api/config/source-system/99999/aas/submodels")
            .then()
            .statusCode(404);
    }
}
