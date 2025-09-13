package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import com.github.tomakehurst.wiremock.WireMockServer;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.RequestExecutionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.ResponseSubscriptionException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RestClient contains a single public method called 'pollJsonObjectFromApi', that takes a prebuilt HttpRequest.
 * 'pollJsonObjectFromApi' is tested in this TestClass.
 * Tests are done by starting a mockServer with which the RestClient interacts in the same way it would with a real Api.
 * Requests are manually constructed to ensure independence from the RequestBuilder.
 */
@DisplayName("RestClientTest: Test of single public method called 'pollJsonObjectFromApi'.")
public class RestClientTest {

    private RestClient restClient;
    private WireMockServer mockServer;
    private WebClient webClient;
    private Vertx vertx;
    private JsonObject expectedJsonObject;

    @BeforeEach
    void setUp() {
        restClient = new RestClient();

        JsonArray dataArrayForExpectedJsonObject = new JsonArray()
                .add(new JsonObject().put("id", 1).put("name", "Test 1"))
                .add(new JsonObject().put("id", 2).put("name", "Test 2"));
        expectedJsonObject = new JsonObject()
                .put("requestMatched", true)
                .put("message", "All parameters matched correctly")
                .put("array", dataArrayForExpectedJsonObject);

        mockServer = new WireMockServer(8089);
        mockServer.start();
        this.createMockServerStubsThatContainsGivenJsonObject(expectedJsonObject);

        vertx = Vertx.vertx();
        webClient = WebClient.create(vertx);
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
        webClient.close();
        vertx.close();
    }

    @Test
    @DisplayName("Api Call with RequestType GET returned expectedJsonObject.")
    void testGetRequestReturnsCorrectJsonBody() {
        // Arrange
        final HttpRequest<Buffer> request = webClient
                .get(8089, "localhost", "/api/companies/123/projects/456/tasks")
                .addQueryParam("priority", "high")
                .putHeader("Authorization", "Bearer token123abc");

        // Act
        JsonObject resultJsonObject = actByCallingRestClientAndReturnResultJsonObject(request);

        // Assert
        assertResultJsonObjectEqualsExpectedJsonObject(resultJsonObject, expectedJsonObject);
        mockServerVerifyGetRequest();
    }

    @Test
    @DisplayName("Api Call with RequestType POST returned expectedJsonObject.")
    void testPostRequestReturnsCorrectJsonBody() {
        //Arrange
        final HttpRequest<Buffer> request = webClient
                .post(8089, "localhost", "/api/companies/123/projects/456/tasks")
                .addQueryParam("priority", "high")
                .putHeader("Authorization", "Bearer token123abc");

        //Act
        JsonObject resultJsonObject = actByCallingRestClientAndReturnResultJsonObject(request);

        //Assert
        assertResultJsonObjectEqualsExpectedJsonObject(resultJsonObject, expectedJsonObject);
        mockServerVerifyPostRequest();
    }

    @Test
    @DisplayName("Api Call with RequestType PUT returned expectedJsonObject.")
    void testPutRequestReturnsCorrectJsonBody() {
        //Arrange
        final HttpRequest<Buffer> request = webClient
                .put(8089, "localhost", "/api/companies/123/projects/456/tasks")
                .addQueryParam("priority", "high")
                .putHeader("Authorization", "Bearer token123abc");

        //Act
        JsonObject resultJsonObject = actByCallingRestClientAndReturnResultJsonObject(request);

        //Assert
        assertResultJsonObjectEqualsExpectedJsonObject(resultJsonObject, expectedJsonObject);
        mockServerVerifyPutRequest();
    }

    @Test
    @DisplayName("RequestExecutionException thrown because of an invalid Host.")
    void testRequestWithInvalidHostThrowsRequestExecutionException(){
        //Arrange
        final HttpRequest<Buffer> request = webClient
                .put(8089, "wronghost", "/api/companies/123/projects/456/tasks")
                .addQueryParam("priority", "high")
                .putHeader("Authorization", "Bearer token123abc");

        //Act + Assert
        assertThrows(RequestExecutionException.class, () -> restClient.pollJsonObjectFromApi(request));
    }

    @Test
    @DisplayName("RequestExecutionException thrown because of an invalid Port.")
    void testRequestWithInvalidPortThrowsRequestExecutionException(){
        //Arrange
        final HttpRequest<Buffer> request = webClient
                .put(23854, "localhost", "/api/companies/123/projects/456/tasks")
                .addQueryParam("priority", "high")
                .putHeader("Authorization", "Bearer token123abc");

        //Act + Assert
        assertThrows(RequestExecutionException.class, () -> restClient.pollJsonObjectFromApi(request));
    }

    /**
     * Tries to call RestClient with the arranged request in a try-catch-block. Should immediately fail if an Exception occurs.
     * @param arrangedRequest executed in the restClient
     * @return the result of the restClient method call: A polled JsonObject
     */
    private JsonObject actByCallingRestClientAndReturnResultJsonObject(HttpRequest<Buffer> arrangedRequest) {
        try{
            //Act
            return restClient.pollJsonObjectFromApi(arrangedRequest);
        } catch (RequestExecutionException e) {
            fail("RequestExecutionException thrown unexpectedly with this Message: " + e.getMessage(), e.getCause());
        } catch (ResponseSubscriptionException e) {
            fail("ResponseSubscriptionException thrown unexpectedly with this Message: " + e.getMessage(), e.getCause());
        }
        fail("A very unexpected Exception lead to the test failing. If this ever occurs it suggests a major implementation flaw in this class: \n" +
                "stay-in-sync-core/core-polling-node/src/main/java/de/unistuttgart/stayinsync/pollingnode/execution/ressource/RestClient.java");
        return null;
    }

    /**
     * Creates three mockServerStubs with same data, QueryParam and Headers for each of the three RequestTypes (GET,POST,PUT)
     * @param bodyJsonObject contains the String, that is the body the Stub will contain
     */
    private void createMockServerStubsThatContainsGivenJsonObject(final JsonObject bodyJsonObject) {
        mockServer.stubFor(get(urlPathEqualTo("/api/companies/123/projects/456/tasks"))
                .withQueryParam("priority", matching("high|medium|low"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Response-Time", "150ms")
                        .withBody(bodyJsonObject.toString())));
        mockServer.stubFor(post(urlPathEqualTo("/api/companies/123/projects/456/tasks"))
                .withQueryParam("priority", matching("high|medium|low"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Response-Time", "150ms")
                        .withBody(bodyJsonObject.toString())));
        mockServer.stubFor(put(urlPathEqualTo("/api/companies/123/projects/456/tasks"))
                .withQueryParam("priority", matching("high|medium|low"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Response-Time", "150ms")
                        .withBody(bodyJsonObject.toString())));
    }
    /**
     * Verifies if mockServers GET Stub was called with the QueryParam and Header
     */
    private void mockServerVerifyGetRequest() {
        mockServer.verify(getRequestedFor(urlPathEqualTo("/api/companies/123/projects/456/tasks"))
                .withQueryParam("priority", equalTo("high"))
                .withHeader("Authorization", containing("Bearer")));
    }
    /**
     * Verifies if mockServers POST Stub was called with the QueryParam and Header
     */
    private void mockServerVerifyPostRequest() {
        mockServer.verify(postRequestedFor(urlPathEqualTo("/api/companies/123/projects/456/tasks"))
                .withQueryParam("priority", equalTo("high"))
                .withHeader("Authorization", containing("Bearer")));
    }
    /**
     * Verifies if mockServers PUT Stub was called with the QueryParam and Header
     */
    private void mockServerVerifyPutRequest() {
        mockServer.verify(putRequestedFor(urlPathEqualTo("/api/companies/123/projects/456/tasks"))
                .withQueryParam("priority", equalTo("high"))
                .withHeader("Authorization", containing("Bearer")));
    }

    /**
     * Asserts that all values of the expectedJsonObject and the resultJsonObject are equal.
     * @param expectedJsonObject created in arranging process
     * @param resultJsonObject created in acting process
     */
    private void assertResultJsonObjectEqualsExpectedJsonObject(final JsonObject resultJsonObject, final JsonObject expectedJsonObject) {
        assertEquals(resultJsonObject.getBoolean("requestMatched"), expectedJsonObject.getBoolean("requestMatched"));
        assertEquals(resultJsonObject.getString("message"), expectedJsonObject.getString("message"));
    }

}