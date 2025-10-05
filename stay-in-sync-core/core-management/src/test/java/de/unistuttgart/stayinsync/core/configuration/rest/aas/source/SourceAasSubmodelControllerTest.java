package de.unistuttgart.stayinsync.core.configuration.rest.aas.source;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
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

@QuarkusTest
public class SourceAasSubmodelControllerTest {

    @InjectMock
    AasTraversalClient traversal;

    @InjectMock
    SourceSystemAasService aasService;

    @InjectMock
    HttpHeaderBuilder headerBuilder;

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private HttpResponse<Buffer> mockResponse(int statusCode, String body) {
        HttpResponse<Buffer> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(statusCode);
        Mockito.when(response.statusMessage()).thenReturn("OK");
        Mockito.when(response.bodyAsString()).thenReturn(body);
        return response;
    }

    @Test
    void testCreateSubmodel_Success() {
        // Given
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

        // When & Then
        given()
            .contentType("application/json")
            .body(submodelJson)
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/submodels", sourceSystemId)
            .then()
            .statusCode(201)
            .body(equalTo(responseBody));
    }

    @Test
    void testCreateSubmodel_BadRequest() {
        // Given
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
        Mockito.when(aasService.mapHttpError(400, "OK", "Bad Request"))
               .thenReturn(jakarta.ws.rs.core.Response.status(400).entity("Bad Request").build());

        // When & Then
        given()
            .contentType("application/json")
            .body(invalidJson)
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/submodels", sourceSystemId)
            .then()
            .statusCode(400);
    }

    @Test
    void testUpdateSubmodel_Success() {
        // Given
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

        // When & Then
        given()
            .contentType("application/json")
            .body(submodelJson)
            .when()
            .put("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}", 1L, smId)
            .then()
            .statusCode(200)
            .body(equalTo(responseBody));
    }

    @Test
    void testDeleteSubmodel_Success() {
        // Given
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

        // When & Then
        given()
            .when()
            .delete("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}", 1L, smId)
            .then()
            .statusCode(200);
    }

    @Test
    void testDeleteSubmodel_NotFound() {
        // Given
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
        Mockito.when(aasService.mapHttpError(404, "OK", "Submodel not found"))
               .thenReturn(jakarta.ws.rs.core.Response.status(404).entity("Submodel not found").build());

        // When & Then
        given()
            .when()
            .delete("/api/config/source-system/{sourceSystemId}/aas/submodels/{smId}", 1L, smId)
            .then()
            .statusCode(404);
    }

    @Test
    void testCreateSubmodel_NonExistentSourceSystem() {
        // Given
        String submodelJson = "{\"id\":\"test-submodel\",\"idShort\":\"TestSubmodel\",\"modelType\":\"Submodel\"}";
        
        Mockito.when(aasService.validateAasSource(Mockito.isNull()))
                .thenThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException(
                        jakarta.ws.rs.core.Response.Status.NOT_FOUND, "Source system not found", "Source system is null"));
        
        // Mock the error mapping service to return the correct HTTP response
        jakarta.ws.rs.core.Response errorResponse = jakarta.ws.rs.core.Response.status(404)
                .entity("Source system not found")
                .build();
        Mockito.when(aasService.mapHttpError(404, "Source system not found", "Source system is null"))
               .thenReturn(errorResponse);

        // When & Then
        given()
            .contentType("application/json")
            .body(submodelJson)
            .when()
            .post("/api/config/source-system/99999/aas/submodels")
            .then()
            .statusCode(404);
    }
}
