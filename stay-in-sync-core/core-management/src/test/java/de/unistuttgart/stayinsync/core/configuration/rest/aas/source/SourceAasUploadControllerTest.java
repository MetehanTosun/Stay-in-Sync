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

/**
 * Integration tests for {@link de.unistuttgart.stayinsync.core.configuration.rest.aas.source.SourceAasUploadController}.
 * Verifies upload and attachment functionality for AASX files in Source Systems.
 * Tests include upload success, error handling, and operations with non-existent source systems.
 */
@QuarkusTest
public class SourceAasUploadControllerTest {

    @InjectMock
    AasStructureSnapshotService snapshotService;

    private SourceSystem sourceSystem;

    /**
     * Initializes a test SourceSystem entity and enables detailed RestAssured logging
     * in case of validation failures.
     */
    @BeforeEach
    @Transactional
    void setUp() {
        sourceSystem = new SourceSystem();
        sourceSystem.apiType = "AAS";
        sourceSystem.apiUrl = "http://aas.example";
        sourceSystem.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        sourceSystem.name = "test-source";
        sourceSystem.persist();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Creates a mock HttpResponse object with a specified status code and response body.
     *
     * @param statusCode The HTTP status code for the mock response.
     * @param body The response body content as a string.
     * @return A mocked {@link HttpResponse} instance configured with the given data.
     */
    private HttpResponse<Buffer> mockResponse(int statusCode, String body) {
        HttpResponse<Buffer> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(statusCode);
        Mockito.when(response.statusMessage()).thenReturn("OK");
        Mockito.when(response.bodyAsString()).thenReturn(body);
        return response;
    }

    /**
     * Tests successful upload of an AASX file to a Source System.
     * Verifies that the upload endpoint returns HTTP 202 Accepted upon successful processing.
     */
    @Test
    void testUploadAasx_Success() {
        String aasxContent = "test-aasx-content";
        int attachedCount = 2;
        
        Mockito.when(snapshotService.attachSubmodelsLive(any(Long.class), any(byte[].class)))
               .thenReturn(attachedCount);

        given()
            .multiPart("file", "test.aasx", aasxContent.getBytes())
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/upload", sourceSystem.id)
            .then()
            .statusCode(202);
    }

    /**
     * Tests behavior when uploading an invalid AASX file.
     * Verifies that a RuntimeException results in a 500 Internal Server Error response.
     */
    @Test
    void testUploadAasx_BadRequest() {
        Mockito.when(snapshotService.attachSubmodelsLive(any(Long.class), any(byte[].class)))
               .thenThrow(new RuntimeException("Invalid AASX file"));

        given()
            .multiPart("file", "invalid.aasx", "invalid-content".getBytes())
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/upload", sourceSystem.id)
            .then()
            .statusCode(500);
    }

    /**
     * Tests attaching selected submodels from an uploaded AASX file.
     * Verifies that the service correctly processes the selection JSON and returns 202 Accepted.
     */
    @Test
    void testAttachSelectedAasx_Success() {
        String aasxContent = "test-aasx-content";
        String selectionJson = "{\"submodels\":[{\"id\":\"test-submodel\",\"full\":true,\"elements\":[]}]}";
        int attachedCount = 1;
        
        Mockito.when(snapshotService.attachSelectedFromAasx(any(Long.class), any(byte[].class), any(io.vertx.core.json.JsonObject.class)))
               .thenReturn(attachedCount);

        given()
            .multiPart("file", "test.aasx", aasxContent.getBytes())
            .multiPart("selection", selectionJson, "application/json")
            .when()
            .post("/api/config/source-system/{sourceSystemId}/aas/upload/attach-selected", sourceSystem.id)
            .then()
            .statusCode(202);
    }

    /**
     * Tests behavior when trying to attach submodels from an AASX file to a non-existent Source System.
     * Verifies that the endpoint still responds with HTTP 202 to maintain consistent API behavior.
     */
    @Test
    void testAttachSelectedAasx_NonExistentSourceSystem() {
        String aasxContent = "test-aasx-content";
        String selectionJson = "{\"submodels\":[{\"id\":\"test-submodel\",\"full\":true,\"elements\":[]}]}";

        given()
            .multiPart("file", "test.aasx", aasxContent.getBytes())
            .multiPart("selection", selectionJson, "application/json")
            .when()
            .post("/api/config/source-system/99999/aas/upload/attach-selected")
            .then()
            .statusCode(202);
    }

    /**
     * Tests upload behavior when the Source System ID does not exist.
     * Verifies that the system returns HTTP 202 Accepted as a default non-blocking response.
     */
    @Test
    void testUploadAasx_NonExistentSourceSystem() {
        String aasxContent = "test-aasx-content";

        given()
            .multiPart("file", "test.aasx", aasxContent.getBytes())
            .when()
            .post("/api/config/source-system/99999/aas/upload")
            .then()
            .statusCode(202);
    }
}
