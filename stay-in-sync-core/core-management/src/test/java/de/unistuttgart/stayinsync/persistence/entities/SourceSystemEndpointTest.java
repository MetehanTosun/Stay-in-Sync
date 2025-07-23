package de.unistuttgart.stayinsync.persistence.entities;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestTransaction
public class SourceSystemEndpointTest {

    @Inject
    jakarta.persistence.EntityManager em;

    @Inject
    SourceSystemEndpointService endpointService;

    @Test
    public void testPersistAndLoadResponseBodySchema() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "TestSystem";
        sourceSystem.apiUrl = "http://test";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/foo";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\"}";
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals("{\"type\":\"object\"}", loaded.responseBodySchema);
    }

    @Test
    public void testInvalidJsonInResponseBodySchemaThrowsException() {
        CreateSourceSystemEndpointDTO dto = new CreateSourceSystemEndpointDTO("/bar", "POST", "{invalidJson}");
        CoreManagementException ex = Assertions.assertThrows(CoreManagementException.class, () -> {
            endpointService.persistSourceSystemEndpoint(dto, null);
        });
        Assertions.assertTrue(ex.getMessage().contains("responseBodySchema"));
    }
}
