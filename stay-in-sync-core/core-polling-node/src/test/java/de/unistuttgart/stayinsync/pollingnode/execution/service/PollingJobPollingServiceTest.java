package java.de.unistuttgart.stayinsync.pollingnode.execution.service;

import java.de.unistuttgart.stayinsync.pollingnode.execution.ressource.RestClient;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.de.unistuttgart.stayinsync.pollingnode.execution.service.PollingJobPollingService;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PollingJobPollingServiceTest {

    private RestClient restClient;
    private PollingJobPollingService service;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        service = new PollingJobPollingService(restClient);
    }

    @Test
    void testPollAndMapDataSuccessfulPolling() throws Exception {
        String apiAddress = "https://example.com";
        JsonObject jsonObject = new JsonObject()
                .put("field1", "value1")
                .put("field2", 42);

        when(restClient.getJsonOfApi(apiAddress))
                .thenReturn(io.smallrye.mutiny.Uni.createFrom().item(jsonObject));

        Map<String, Object> result = service.pollAndMapData(apiAddress);

        assertEquals(2, result.size());
        assertEquals("value1", result.get("field1"));
        assertEquals(42, result.get("field2"));
    }

    @Test
    void testPollAndMapDataPollingFails() throws Exception {
        String apiAddress = "https://example.com";

        when(restClient.getJsonOfApi(apiAddress))
                .thenReturn(io.smallrye.mutiny.Uni.createFrom().failure(new RuntimeException("Fehler")));

        Map<String, Object> result = service.pollAndMapData(apiAddress);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchJsonDataSuccessful() throws Exception {
        String apiAddress = "https://example.com";
        JsonObject jsonObject = new JsonObject().put("key", "value");

        when(restClient.getJsonOfApi(apiAddress))
                .thenReturn(io.smallrye.mutiny.Uni.createFrom().item(jsonObject));

        Optional<JsonObject> result = service.fetchJsonData(apiAddress);

        assertTrue(result.isPresent());
        assertEquals("value", result.get().getString("key"));
    }

    @Test
    void testFetchJsonDataFailure() throws Exception {
        String apiAddress = "https://example.com";

        when(restClient.getJsonOfApi(apiAddress))
                .thenReturn(io.smallrye.mutiny.Uni.createFrom().failure(new RuntimeException("Fehler")));

        Optional<JsonObject> result = service.fetchJsonData(apiAddress);

        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertJsonObjectToFieldValueMappingEmptyJson() throws Exception {
        JsonObject emptyJson = new JsonObject();

        // Zugriff auf private Methode über Reflection
        var method = PollingJobPollingService.class.getDeclaredMethod(
                "convertJsonObjectToFieldValueMapping", JsonObject.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(service, emptyJson);

        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertJsonObjectToFieldValueMappingValidJson() throws Exception {
        JsonObject json = new JsonObject()
                .put("fieldA", "test")
                .put("fieldB", 123);

        var method = PollingJobPollingService.class.getDeclaredMethod(
                "convertJsonObjectToFieldValueMapping", JsonObject.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(service, json);

        assertEquals(2, result.size());
        assertEquals("test", result.get("fieldA"));
        assertEquals(123, result.get("fieldB"));
    }
}
