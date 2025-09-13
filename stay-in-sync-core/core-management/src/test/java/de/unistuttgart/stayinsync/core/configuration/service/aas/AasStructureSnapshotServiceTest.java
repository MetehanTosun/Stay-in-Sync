package de.unistuttgart.stayinsync.core.configuration.service.aas;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
public class AasStructureSnapshotServiceTest {

    @InjectMock
    AasTraversalClient traversal;

    @Inject
    AasStructureSnapshotService snapshot;

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    @Transactional
    void init() {
        AasElementLite.deleteAll();
        AasSubmodelLite.deleteAll();
        SourceSystem.deleteAll();
        SourceSystem ss = new SourceSystem();
        ss.apiType = "AAS";
        ss.apiUrl = "http://aas.example";
        ss.aasId = "aHR0cHM6Ly9leGFtcGxlLmNvbS9pZHMvYWFzLzAzMDBfNjE0MV81MDUyXzg3MTU";
        ss.name = "test";
        ss.persist();
    }

    @Test
    void refreshSnapshot_persistsSubmodelsAndElements() {
        String submodelRefs = "{\"result\":[{\"type\":\"ModelReference\",\"keys\":[{\"type\":\"Submodel\",\"value\":\"https://example.com/ids/sm/1\"}]}]}";
        HttpResponse<Buffer> refsResp = mockResp(200, submodelRefs);
        Mockito.when(traversal.listSubmodels(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(refsResp));

        String smFull = "{\"id\":\"https://example.com/ids/sm/1\",\"idShort\":\"sm1\",\"kind\":\"Instance\"}";
        HttpResponse<Buffer> smFullResp = mockResp(200, smFull);
        Mockito.when(traversal.getSubmodel(anyString(), anyString(), anyMap())).thenReturn(Uni.createFrom().item(smFullResp));

        String elements = "{\"result\":[{\"modelType\":\"Property\",\"idShort\":\"p\",\"valueType\":\"xs:string\"}]}";
        HttpResponse<Buffer> elResp = mockResp(200, elements);
        Mockito.when(traversal.listElements(anyString(), anyString(), anyString(), any(), anyMap())).thenReturn(Uni.createFrom().item(elResp));

        Long id = SourceSystem.<SourceSystem>findAll().firstResult().id;
        snapshot.refreshSnapshot(id);

        assertThat(AasSubmodelLite.count("sourceSystem.id", id)).isEqualTo(1);
        AasSubmodelLite sm = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id", id).firstResult();
        assertThat(sm.submodelIdShort).isEqualTo("sm1");
        assertThat(AasElementLite.count("submodelLite.id", sm.id)).isEqualTo(1);
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



