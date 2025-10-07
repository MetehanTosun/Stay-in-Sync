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
public class SourceAasTestControllerTest {

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
    void testAasTest_Success() {
        // Given
        Long sourceSystemId = 1L;
        String responseBody = "{\"idShort\":\"shell\",\"assetKind\":\"asset\"}";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = sourceSystemId;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(200, responseBody);
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.getShell(anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));

        // When & Then
        given()
            .contentType("application/json")
            .body("{}")
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/test", sourceSystemId)
            .then()
            .statusCode(200)
            .body(equalTo(responseBody));
    }

    @Test
    void testAasTest_ConnectionFailed() {
        // Given
        Long sourceSystemId = 1L;
        String responseBody = "{\"status\":\"error\",\"message\":\"Connection failed\"}";
        
        SourceSystem mockSystem = new SourceSystem();
        mockSystem.id = sourceSystemId;
        mockSystem.apiType = "AAS";
        mockSystem.apiUrl = "http://aas.example";
        mockSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        mockSystem.name = "test-source";
        
        HttpResponse<Buffer> mockResponse = mockResponse(500, responseBody);
        Mockito.when(aasService.validateAasSource(any(SourceSystem.class))).thenReturn(mockSystem);
        Mockito.when(headerBuilder.buildMergedHeaders(any(SourceSystem.class), any(HttpHeaderBuilder.Mode.class)))
               .thenReturn(Map.of("Authorization", "Bearer token"));
        Mockito.when(traversal.getShell(anyString(), anyString(), anyMap()))
               .thenReturn(Uni.createFrom().item(mockResponse));
        Mockito.doThrow(new de.unistuttgart.stayinsync.core.configuration.service.CoreManagementWebException(500, "OK", responseBody))
                .when(aasService).throwHttpError(500, "OK", responseBody);

        // When & Then
        given()
            .contentType("application/json")
            .body("{}")
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/test", sourceSystemId)
            .then()
            .statusCode(500);
    }

    @Test
    void testAasTest_NonExistentSourceSystem() {
        // Given
        Mockito.when(aasService.validateAasSource(Mockito.isNull()))
                .thenThrow(new de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException(
                        jakarta.ws.rs.core.Response.Status.NOT_FOUND, "Source system not found", "Source system is null"));
        
        // Mock the error mapping service to return the correct HTTP response
        Mockito.doThrow(new de.unistuttgart.stayinsync.core.configuration.service.CoreManagementWebException(404, "Source system not found", "Source system is null"))
                .when(aasService).throwHttpError(404, "Source system not found", "Source system is null");

        // When & Then
        given()
            .contentType("application/json")
            .body("{}")
            .when()
            .post("/api/config/source-system/99999/aas/test")
            .then()
            .statusCode(404);
    }
}
