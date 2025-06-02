package de.unistuttgart.stayinsync.pollingnode.ressource;


import de.unistuttgart.stayinsync.pollingnode.entities.ApiAddress;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class RestClient {

    private final HttpClient httpClient;

    /**
     * Constructs client for later requests with custom Executor to control the maximum number of
     * Requests runnable parallel.
     */
    public RestClient() {
        super();
        ExecutorService httpExecutor = Executors.newFixedThreadPool(200);
        this.httpClient = HttpClient.newBuilder()
                .executor(httpExecutor)
                .connectTimeout(Duration.ofMillis(200)) // TCP Connect-Timeout
                .build();
    }

    /**
     * Polls json and returns it as CompletableFuture. Therefore, method should be called asynchronously.
     * @param apiAddress contains address of RestAPI to connect to.
     * @return json as String as CompletableFuture
     */
    @GET
    public CompletableFuture<String> getJsonDataOf(ApiAddress apiAddress) {
        HttpRequest request;
        try {
            request = createHttpRequest(apiAddress);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid URI in ApiAddress: " + apiAddress.getString());
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(300, TimeUnit.MILLISECONDS)
                .thenApply(HttpResponse::body);
    }

    /*
    * Creates HttpRequest out of ApiAddress
    * @param apiAddress contains single String with address
     */
    private HttpRequest createHttpRequest(final ApiAddress apiAddress) throws IllegalArgumentException {
            return HttpRequest.newBuilder()
                    .uri(URI.create(apiAddress.getString()))
                    .timeout(Duration.ofMillis(10))
                    .GET()
                    .build();
    }

    /*
     * Converts received json into a simpler format for later processes.
     */
    private void handleReceivedJson(final ApiAddress apiAddress, final String json) {
        // Beispiel: Weiterleiten oder Parsen
        System.out.println("Received JSON from " + apiAddress.getString() + ": " + json);
        // TODO: Event weitergeben, an zentrale Klasse melden, verarbeiten etc.
    }

    /*
     * Exception handling for unsuccessful requests.
     */
    private void handleRequestFailure(ApiAddress address, Throwable ex) {
        System.err.println("Request to " + address.getString() + " failed: " + ex.getMessage());
        // TODO: Retry, Monitoring, Logging, Blacklisting etc.
    }


}
