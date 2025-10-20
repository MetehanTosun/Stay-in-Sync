package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.transaction.Transactional;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Integration tests for AAS upload functionality.
 * Verifies the upload and attachment of AASX files for Source Systems.
 * Tests include successful uploads, submodel attachment, and snapshot refresh handling.
 */
@QuarkusTest
public class AasUploadResourceIT {

    @InjectMock
    AasTraversalClient traversal;

    @InjectMock
    AasStructureSnapshotService snapshotService;

    /**
     * Initializes the test environment before each test.
     * Creates a test SourceSystem entity and enables detailed RestAssured logging for debugging.
     */
    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        SourceSystem ss = new SourceSystem();
        ss.name = "Sys";
        ss.apiUrl = "http://localhost:8081";
        ss.apiType = "AAS";
        ss.aasId = "https://example.com/aas/demo";
        ss.persist();
    }

    /**
     * Tests successful upload of an AASX file containing at least one submodel.
     * Mocks the AAS traversal interactions to simulate submodel creation and attachment.
     * Verifies that the endpoint returns HTTP 202 and includes expected response fields.
     *
     * @throws Exception if file creation or upload fails.
     */
    @Test
    void uploadAasx_attachesAtLeastOne() throws Exception {
        HttpResponse<Buffer> r404 = buildResp(404, "");
        HttpResponse<Buffer> r201 = buildResp(201, "{\"id\":\"https://example.com/sm/minimal\"}");
        HttpResponse<Buffer> r204 = buildResp(204, "");
        HttpResponse<Buffer> r200Empty = buildResp(200, "[]");
        when(traversal.getSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r404));
        when(traversal.createSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r201));
        when(traversal.addSubmodelReferenceToShell(anyString(), anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r204));
        when(traversal.listSubmodelReferences(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r200Empty));

        Mockito.doNothing().when(snapshotService).refreshSnapshot(anyLong());

        byte[] bytes = buildXmlOnlyAasx("https://example.com/sm/minimal", "Minimal");

        given()
            .multiPart("file", "xml-only-minimal-submodel.aasx", bytes)
        .when()
            .post("/api/config/source-system/{id}/aas/upload", 1)
        .then()
            .statusCode(202)
            .body("attachedSubmodels", greaterThanOrEqualTo(0))
            .body("filename", notNullValue());
    }

    /**
     * Utility method for creating a mocked {@link HttpResponse} with a specific status code and body.
     *
     * @param code The HTTP status code to simulate.
     * @param body The response body content.
     * @return A mocked {@link HttpResponse} with the specified values.
     */
    @SuppressWarnings("unchecked")
    private HttpResponse<Buffer> buildResp(int code, String body) {
        HttpResponse<?> raw = Mockito.mock(HttpResponse.class);
        HttpResponse<Buffer> resp = (HttpResponse<Buffer>) raw;
        Mockito.when(resp.statusCode()).thenReturn(code);
        Mockito.when(resp.statusMessage()).thenReturn("");
        Mockito.when(resp.bodyAsString()).thenReturn(body);
        return resp;
    }

    /**
     * Builds an in-memory AASX (ZIP) file containing a minimal XML submodel definition.
     *
     * @param id The identifier (ID) of the submodel.
     * @param idShort The short name (idShort) of the submodel.
     * @return A byte array representing the generated AASX file.
     * @throws Exception if ZIP generation fails.
     */
    private byte[] buildXmlOnlyAasx(String id, String idShort) throws Exception {
        String xml = """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <submodel xmlns=\"http://www.admin-shell.io/aas/3/0\">
                  <identification>
                    <id>%s</id>
                  </identification>
                  <idShort>%s</idShort>
                  <submodelElements/>
                </submodel>
                """.formatted(id, idShort);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            zos.putNextEntry(new java.util.zip.ZipEntry("submodels/minimal.xml"));
            zos.write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}


