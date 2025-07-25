

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
    public void testPersistAndLoadRequestBodySchema() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "TestSystem";
        sourceSystem.apiUrl = "http://test";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/foo";
        endpoint.httpRequestType = "GET";
        endpoint.requestBodySchema = "{\"type\":\"object\"}";
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals("{\"type\":\"object\"}", loaded.requestBodySchema);
    }

    @Test
    public void testInvalidJsonInRequestBodySchemaThrowsException() {
        CreateSourceSystemEndpointDTO dto = new CreateSourceSystemEndpointDTO("/bar", "POST", "{invalidJson}");
        CoreManagementException ex = Assertions.assertThrows(CoreManagementException.class, () -> {
            endpointService.persistSourceSystemEndpoint(dto, null);
        });
        Assertions.assertTrue(ex.getMessage().contains("requestBodySchema"));
    }

    @Test
    public void testPersistAndLoadComplexRequestBodySchema() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "ComplexSystem";
        sourceSystem.apiUrl = "http://complex";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        String complexSchema = "{" +
                "\"type\":\"object\"," +
                "\"properties\":{\"foo\":{\"type\":\"string\"},\"bar\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}}," +
                "\"required\":[\"foo\"]" +
                "}";

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/complex";
        endpoint.httpRequestType = "POST";
        endpoint.requestBodySchema = complexSchema;
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals(complexSchema, loaded.requestBodySchema);
    }

    @Test
    public void testUpdateRequestBodySchema() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "UpdateSystem";
        sourceSystem.apiUrl = "http://update";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/update";
        endpoint.httpRequestType = "PUT";
        endpoint.requestBodySchema = "{\"type\":\"string\"}";
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        Long id = endpoint.id;
        em.clear();

        SourceSystemEndpoint toUpdate = em.find(SourceSystemEndpoint.class, id);
        toUpdate.requestBodySchema = "{\"type\":\"number\"}";
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, id);
        Assertions.assertEquals("{\"type\":\"number\"}", loaded.requestBodySchema);
    }

    @Test
    public void testMultipleEndpointsWithDifferentSchemas() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "MultiSystem";
        sourceSystem.apiUrl = "http://multi";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint ep1 = new SourceSystemEndpoint();
        ep1.endpointPath = "/ep1";
        ep1.httpRequestType = "GET";
        ep1.requestBodySchema = "{\"type\":\"object\"}";
        ep1.sourceSystem = sourceSystem;
        ep1.syncSystem = sourceSystem;
        em.persist(ep1);

        SourceSystemEndpoint ep2 = new SourceSystemEndpoint();
        ep2.endpointPath = "/ep2";
        ep2.httpRequestType = "POST";
        ep2.requestBodySchema = "{\"type\":\"array\"}";
        ep2.sourceSystem = sourceSystem;
        ep2.syncSystem = sourceSystem;
        em.persist(ep2);

        em.flush();
        em.clear();

        SourceSystemEndpoint loaded1 = em.find(SourceSystemEndpoint.class, ep1.id);
        SourceSystemEndpoint loaded2 = em.find(SourceSystemEndpoint.class, ep2.id);
        Assertions.assertEquals("{\"type\":\"object\"}", loaded1.requestBodySchema);
        Assertions.assertEquals("{\"type\":\"array\"}", loaded2.requestBodySchema);
    }
}

