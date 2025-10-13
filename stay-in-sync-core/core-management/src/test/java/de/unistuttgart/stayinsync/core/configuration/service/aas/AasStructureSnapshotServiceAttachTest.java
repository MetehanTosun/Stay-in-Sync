package de.unistuttgart.stayinsync.core.configuration.service.aas;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AasStructureSnapshotServiceAttachTest {

    @Inject
    AasStructureSnapshotService service;

    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    AasTraversalClient traversal;

    @BeforeEach
    @Transactional
    void setupDb() {
        // minimal SourceSystem
        SourceSystem ss = new SourceSystem();
        ss.name = "TestSys";
        ss.apiUrl = "http://localhost:8081";
        ss.apiType = "AAS";
        ss.aasId = "https://example.com/aas/demo";
        ss.persist();
    }

    @Test
    void attachesXmlOnlyTemplate() throws Exception {
        byte[] bytes = buildXmlOnlyAasx("https://example.com/sm/minimal", "Minimal");

        // Prepare responses first to avoid nested stubbing
        HttpResponse<Buffer> r404 = buildResp(404, "");
        HttpResponse<Buffer> r201 = buildResp(201, "{\"id\":\"https://example.com/sm/minimal\"}");
        HttpResponse<Buffer> r204 = buildResp(204, "");
        HttpResponse<Buffer> r200Empty = buildResp(200, "[]");
        HttpResponse<Buffer> r200WithRef = buildResp(200, "[{\"type\":\"ModelReference\",\"keys\":[{\"type\":\"Submodel\",\"value\":\"https://example.com/sm/minimal\"}]}]");

        // mock: submodel not found initially
        when(traversal.getSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r404));
        // mock create response with id in body
        when(traversal.createSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r201));
        // mock add ref
        when(traversal.addSubmodelReferenceToShell(anyString(), anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r204));
        // mock list refs after add
        when(traversal.listSubmodelReferences(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(r200Empty), Uni.createFrom().item(r200WithRef));

        int attached = service.attachSubmodelsLive(SourceSystem.<SourceSystem>findAll().firstResult().id, bytes);
        assertThat(attached).isGreaterThanOrEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Buffer> buildResp(int code, String body) {
        HttpResponse<?> raw = Mockito.mock(HttpResponse.class);
        HttpResponse<Buffer> resp = (HttpResponse<Buffer>) raw;
        when(resp.statusCode()).thenReturn(code);
        when(resp.statusMessage()).thenReturn("");
        when(resp.bodyAsString()).thenReturn(body);
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("submodels/minimal.xml"));
            zos.write(xml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
