package de.unistuttgart.stayinsync.core.configuration.rest.aas.source;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder;
import de.unistuttgart.stayinsync.core.configuration.service.aas.SourceSystemAasService;
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

import java.io.ByteArrayInputStream;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
public class SourceAasUploadControllerTest {

    @InjectMock
    AasStructureSnapshotService snapshotService;

    private SourceSystem sourceSystem;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up in correct order to avoid foreign key constraints
        sourceSystem = new SourceSystem();
        sourceSystem.apiType = "AAS";
        sourceSystem.apiUrl = "http://aas.example";
        sourceSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        sourceSystem.name = "test-source";
        sourceSystem.persist();
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
    void testUploadAasx_Success() {
        // Given
        String aasxContent = "test-aasx-content";
        int attachedCount = 2;
        
        Mockito.when(snapshotService.attachSubmodelsLive(any(Long.class), any(byte[].class)))
               .thenReturn(attachedCount);

        // When & Then
        given()
            .multiPart("file", "test.aasx", aasxContent.getBytes())
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/upload", sourceSystem.id)
            .then()
            .statusCode(202);
    }

    @Test
    void testUploadAasx_BadRequest() {
        // Given
        Mockito.when(snapshotService.attachSubmodelsLive(any(Long.class), any(byte[].class)))
               .thenThrow(new RuntimeException("Invalid AASX file"));

        // When & Then
        given()
            .multiPart("file", "invalid.aasx", "invalid-content".getBytes())
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/upload", sourceSystem.id)
            .then()
            .statusCode(500);
    }

    @Test
    void testAttachSelectedAasx_Success() {
        // Given
        String aasxContent = "test-aasx-content";
        String selectionJson = "{\"submodels\":[{\"id\":\"test-submodel\",\"full\":true,\"elements\":[]}]}";
        int attachedCount = 1;
        
        Mockito.when(snapshotService.attachSelectedFromAasx(any(Long.class), any(byte[].class), any(io.vertx.core.json.JsonObject.class)))
               .thenReturn(attachedCount);

        // When & Then
        given()
            .multiPart("file", "test.aasx", aasxContent.getBytes())
            .multiPart("selection", selectionJson, "application/json")
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/upload/attach-selected", sourceSystem.id)
            .then()
            .statusCode(202);
    }

    @Test
    void testAttachSelectedAasx_NonExistentSourceSystem() {
        // Given
        String aasxContent = "test-aasx-content";
        String selectionJson = "{\"submodels\":[{\"id\":\"test-submodel\",\"full\":true,\"elements\":[]}]}";

        // When & Then
        given()
            .multiPart("file", "test.aasx", aasxContent.getBytes())
            .multiPart("selection", selectionJson, "application/json")
            .when()
            .post("/api/config/source-system/99999/aas/upload/attach-selected")
            .then()
            .statusCode(202);
    }

    @Test
    void testUploadAasx_NonExistentSourceSystem() {
        // Given
        String aasxContent = "test-aasx-content";

        // When & Then
        given()
            .multiPart("file", "test.aasx", aasxContent.getBytes())
            .when()
            .post("/api/config/source-system/99999/aas/upload")
            .then()
            .statusCode(202);
    }
}
