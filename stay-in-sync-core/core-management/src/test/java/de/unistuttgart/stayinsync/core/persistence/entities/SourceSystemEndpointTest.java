

package de.unistuttgart.stayinsync.core.persistence.entities;

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
        CreateSourceSystemEndpointDTO dto = new CreateSourceSystemEndpointDTO("/bar", "POST", "{invalidJson}", null);
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

    // Additional tests for responseDts field
    @Test
    public void testPersistAndLoadResponseDts() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "TypeScriptSystem";
        sourceSystem.apiUrl = "http://typescript";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/typescript";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"number\"}}}";
        endpoint.responseDts = "interface ResponseBody { id: number; }";
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals("interface ResponseBody { id: number; }", loaded.responseDts);
    }

    @Test
    public void testPersistAndLoadLargeResponseDts() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "LargeTypeScriptSystem";
        sourceSystem.apiUrl = "http://large";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        // Create a large TypeScript interface
        StringBuilder largeTypeScript = new StringBuilder("interface ResponseBody {\n");
        for (int i = 0; i < 100; i++) {
            largeTypeScript.append("  prop").append(i).append(": string;\n");
        }
        largeTypeScript.append("}");

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/large";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\"}";
        endpoint.responseDts = largeTypeScript.toString();
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals(largeTypeScript.toString(), loaded.responseDts);
    }

    @Test
    public void testUpdateResponseDts() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "UpdateTypeScriptSystem";
        sourceSystem.apiUrl = "http://updatets";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/updatets";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\"}";
        endpoint.responseDts = "interface ResponseBody { old: string; }";
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        Long id = endpoint.id;
        em.clear();

        SourceSystemEndpoint toUpdate = em.find(SourceSystemEndpoint.class, id);
        toUpdate.responseDts = "interface ResponseBody { new: number; }";
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, id);
        Assertions.assertEquals("interface ResponseBody { new: number; }", loaded.responseDts);
    }

    @Test
    public void testResponseDtsWithSpecialCharacters() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "SpecialCharSystem";
        sourceSystem.apiUrl = "http://special";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        String specialTypeScript = "interface ResponseBody {\n" +
                "  'user-name': string;\n" +
                "  'email@domain': string;\n" +
                "  $metadata: object;\n" +
                "  'with spaces': number;\n" +
                "}";

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/special";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\"}";
        endpoint.responseDts = specialTypeScript;
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals(specialTypeScript, loaded.responseDts);
    }

    @Test
    public void testResponseDtsWithUnicodeCharacters() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "UnicodeSystem";
        sourceSystem.apiUrl = "http://unicode";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        String unicodeTypeScript = "interface ResponseBody {\n" +
                "  // Comment with émojis 🎉\n" +
                "  name: string; // User's name\n" +
                "  message: string; // Message with 🚀\n" +
                "}";

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/unicode";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\"}";
        endpoint.responseDts = unicodeTypeScript;
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals(unicodeTypeScript, loaded.responseDts);
    }

    @Test
    public void testNullResponseDts() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "NullTypeScriptSystem";
        sourceSystem.apiUrl = "http://null";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/null";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\"}";
        endpoint.responseDts = null;
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertNull(loaded.responseDts);
    }

    @Test
    public void testEmptyResponseDts() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "EmptyTypeScriptSystem";
        sourceSystem.apiUrl = "http://empty";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/empty";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\"}";
        endpoint.responseDts = "";
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals("", loaded.responseDts);
    }

    @Test
    public void testMultipleEndpointsWithDifferentResponseDts() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "MultiTypeScriptSystem";
        sourceSystem.apiUrl = "http://multits";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        SourceSystemEndpoint ep1 = new SourceSystemEndpoint();
        ep1.endpointPath = "/ep1";
        ep1.httpRequestType = "GET";
        ep1.responseBodySchema = "{\"type\":\"object\"}";
        ep1.responseDts = "interface ResponseBody { id: number; }";
        ep1.sourceSystem = sourceSystem;
        ep1.syncSystem = sourceSystem;
        em.persist(ep1);

        SourceSystemEndpoint ep2 = new SourceSystemEndpoint();
        ep2.endpointPath = "/ep2";
        ep2.httpRequestType = "POST";
        ep2.responseBodySchema = "{\"type\":\"object\"}";
        ep2.responseDts = "interface ResponseBody { name: string; }";
        ep2.sourceSystem = sourceSystem;
        ep2.syncSystem = sourceSystem;
        em.persist(ep2);

        em.flush();
        em.clear();

        SourceSystemEndpoint loaded1 = em.find(SourceSystemEndpoint.class, ep1.id);
        SourceSystemEndpoint loaded2 = em.find(SourceSystemEndpoint.class, ep2.id);
        Assertions.assertEquals("interface ResponseBody { id: number; }", loaded1.responseDts);
        Assertions.assertEquals("interface ResponseBody { name: string; }", loaded2.responseDts);
    }

    @Test
    public void testResponseDtsWithComplexTypeScript() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "ComplexTypeScriptSystem";
        sourceSystem.apiUrl = "http://complexts";
        sourceSystem.apiType = "REST";
        em.persist(sourceSystem);

        String complexTypeScript = "interface ResponseBody {\n" +
                "  user: {\n" +
                "    id: number;\n" +
                "    profile: {\n" +
                "      name: string;\n" +
                "      email: string;\n" +
                "    };\n" +
                "  };\n" +
                "  metadata: {\n" +
                "    createdAt: string;\n" +
                "    updatedAt: string;\n" +
                "  };\n" +
                "  items: Array<{\n" +
                "    id: number;\n" +
                "    name: string;\n" +
                "  }>;\n" +
                "}";

        SourceSystemEndpoint endpoint = new SourceSystemEndpoint();
        endpoint.endpointPath = "/complexts";
        endpoint.httpRequestType = "GET";
        endpoint.responseBodySchema = "{\"type\":\"object\"}";
        endpoint.responseDts = complexTypeScript;
        endpoint.sourceSystem = sourceSystem;
        endpoint.syncSystem = sourceSystem;
        em.persist(endpoint);
        em.flush();
        em.clear();

        SourceSystemEndpoint loaded = em.find(SourceSystemEndpoint.class, endpoint.id);
        Assertions.assertEquals(complexTypeScript, loaded.responseDts);
    }
}

