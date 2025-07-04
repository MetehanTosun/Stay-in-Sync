package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.pollingnode.entities.ApiConnectionDetails;
import de.unistuttgart.stayinsync.transport.dto.*;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.core.Vertx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.unistuttgart.stayinsync.pollingnode.execution.ressource.RestClient;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RestClientTest {

    private WebClient webClient;
    private RestClient restClient;

//    @BeforeEach
//    void setUp() {
//        Vertx vertx = Vertx.vertx();
//        webClient = WebClient.create(vertx);
//        restClient = new RestClient(vertx);
//
//    }

    @Test
    @DisplayName("ConfigureRequest reacts correctly to null fields.")
    void testConfigureRequestAllFieldsNull(){
        ApiConnectionDetails apiConnectionDetails = new ApiConnectionDetails(null,null,null,null);
        assertEquals(Optional.empty(), restClient.configureGetRequest(apiConnectionDetails));
    }

    @Test
    @DisplayName("ConfigureRequest reacts correctly to empty fields.")
    void testConfigureGetRequestSuccessfully(){
        // Arrange
        WebClient webClient = mock(WebClient.class);
        HttpRequest<Buffer> requestMock = mock(HttpRequest.class);

        when(webClient.getAbs(anyString())).thenReturn(requestMock);

        RestClient restClient = new RestClient(Vertx.vertx());
        setInternalWebClient(restClient, webClient); // Wie du es schon gemacht hast

        ApiConnectionDetails details = new ApiConnectionDetails(
                new SourceSystemMessageDTO("name", "http://api.com", "type", new ApiAuthConfigurationMessageDTO("key", "Auth-Header")),
                new SourceSystemEndpointMessageDTO("/endpoint", "GET"),
                Set.of(new ApiRequestParameterMessageDTO("param", "value")),
                Set.of(new ApiRequestHeaderMessageDTO("Header", "HeaderValue"))
        );

        // Act
        restClient.configureGetRequest(details);

        // Assert
        verify(webClient).getAbs("http://api.com/endpoint");
        verify(requestMock).putHeader("Auth-Header", "key");
        verify(requestMock).putHeader("Header", "HeaderValue");
        verify(requestMock).addQueryParam("param", "value");
    }


    @Test
    void testCleanupClosesWebClient() {
        restClient.cleanup();

    }
}