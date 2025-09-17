package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AasUploadResourceIT {

    @InjectMock
    AasTraversalClient traversal;

    @BeforeEach
    void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        SourceSystem ss = new SourceSystem();
        ss.name = "Sys";
        ss.apiUrl = "http://localhost:8081";
        ss.apiType = "AAS";
        ss.aasId = "https://example.com/aas/demo";
        ss.persist();
    }

    @Test
    void uploadAasx_attachesAtLeastOne() throws Exception {
        // Mock traversal interactions
        HttpResponse<Buffer> r404 = buildResp(404, "");
        HttpResponse<Buffer> r201 = buildResp(201, "{\"id\":\"https://example.com/sm/minimal\"}");
        HttpResponse<Buffer> r204 = buildResp(204, "");
        HttpResponse<Buffer> r200Empty = buildResp(200, "[]");
        when(traversal.getSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r404));
        when(traversal.createSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r201));
        when(traversal.addSubmodelReferenceToShell(anyString(), anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r204));
        when(traversal.listSubmodelReferences(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r200Empty));

        byte[] bytes = buildXmlOnlyAasx("https://example.com/sm/minimal", "Minimal");

        given()
            .multiPart("file", "xml-only-minimal-submodel.aasx", bytes)
        .when()
            .post("/api/config/source-system/{id}/aas/upload", 1)
        .then()
            .statusCode(202)
            .body("attachedSubmodels", greaterThanOrEqualTo(1))
            .body("filename", notNullValue());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Buffer> buildResp(int code, String body) {
        HttpResponse<?> raw = Mockito.mock(HttpResponse.class);
        HttpResponse<Buffer> resp = (HttpResponse<Buffer>) raw;
        Mockito.when(resp.statusCode()).thenReturn(code);
        Mockito.when(resp.statusMessage()).thenReturn("");
        Mockito.when(resp.bodyAsString()).thenReturn(body);
        return resp;
    }

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


