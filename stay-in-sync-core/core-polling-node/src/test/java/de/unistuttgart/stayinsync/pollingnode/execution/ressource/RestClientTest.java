package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.unistuttgart.stayinsync.pollingnode.execution.ressource.RestClient;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RestClientTest {

    private Vertx vertx;
    private WebClient webClient;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        vertx = mock(Vertx.class);
        webClient = mock(WebClient.class);
        restClient = new RestClient(vertx);

        // Harten Austausch des echten WebClients durch unseren Mock
        setInternalWebClient(restClient, webClient);
    }

    @Test
    void testGetJsonOfApiSuccessfulResponse() {
        String apiAddress = "https://example.com";
        JsonObject expectedJson = new JsonObject().put("key", "value");
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        doReturn(request).when(webClient).getAbs(apiAddress);
        doReturn(Uni.createFrom().item(response)).when(request).send();
        doReturn(200).when(response).statusCode();
        doReturn(expectedJson).when(response).bodyAsJsonObject();

        JsonObject result = restClient.getJsonOfApi(apiAddress).await().indefinitely();

        assertEquals(expectedJson, result);
        verify(webClient).getAbs(apiAddress);
        verify(request).send();
    }

    @Test
    void testGetJsonOfApiNullBody() {
        String apiAddress = "https://example.com";

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        doReturn(request).when(webClient).getAbs(apiAddress);
        doReturn(Uni.createFrom().item(response)).when(request).send();
        doReturn(200).when(response).statusCode();
        doReturn(null).when(response).bodyAsJsonObject();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> restClient.getJsonOfApi(apiAddress).await().indefinitely());

        assertTrue(ex.getMessage().contains("Invalid Json"));
    }

    @Test
    void testGetJsonOfApiHttpError() {
        String apiAddress = "https://example.com";

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        doReturn(request).when(webClient).getAbs(apiAddress);
        doReturn(Uni.createFrom().item(response)).when(request).send();
        doReturn(500).when(response).statusCode();
        doReturn("Internal Server Error").when(response).statusMessage();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> restClient.getJsonOfApi(apiAddress).await().indefinitely());

        assertTrue(ex.getMessage().contains("HTTP Error"));
    }

    @Test
    void testGetJsonOfApiRequestFails() {
        String apiAddress = "https://example.com";

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(webClient).getAbs(apiAddress);
        doReturn(Uni.createFrom().failure(new RuntimeException("Connection Error"))).when(request).send();

        assertThrows(RuntimeException.class,
                () -> restClient.getJsonOfApi(apiAddress).await().indefinitely());
    }

    @Test
    void testCleanupClosesWebClient() {
        restClient.cleanup();
        verify(webClient).close();
    }

    private void setInternalWebClient(RestClient client, WebClient mockClient) {
        try {
            var field = RestClient.class.getDeclaredField("webClient");
            field.setAccessible(true);
            field.set(client, mockClient);
        } catch (Exception e) {
            fail("Error while creating Mock-Web-Client: " + e.getMessage());
        }
    }
}