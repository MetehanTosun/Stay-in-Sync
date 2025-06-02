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
import io.quarkus.logging.Log;

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
    public CompletableFuture<String> getJsonDataOfSourceSystem(final ApiAddress apiAddress) {
        try {
            HttpRequest request = createHttpRequest(apiAddress);

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenCompose(this::handleResponse);

        } catch (IllegalArgumentException e) {
            Log.warn("Invalid URI in ApiAddress: {}", apiAddress.getString(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Creates HttpRequest out of ApiAddress
     * @param apiAddress contains single String with address
     * @return configured HttpRequest
     * @throws IllegalArgumentException if URI is invalid
     */
    private HttpRequest createHttpRequest(final ApiAddress apiAddress) throws IllegalArgumentException {
        return HttpRequest.newBuilder()
                .uri(URI.create(apiAddress.getString()))
                .timeout(Duration.ofSeconds(30)) // Realistischeres Timeout
                .GET()
                .build();
    }

    /**
     * Handles HTTP response with proper error handling
     * @param response HTTP response to process
     * @return CompletableFuture with response body or failed future
     */
    private CompletableFuture<String> handleResponse(final HttpResponse<String> response) {
        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            Log.info(String.format("Successful poll with status code %d for URI: %s", statusCode, response.uri()));
            return CompletableFuture.completedFuture(response.body());
        } else {
            String errorMessage = String.format("HTTP error %d for URI: %s", statusCode, response.uri());
            Log.error(errorMessage);
            return CompletableFuture.failedFuture(new RuntimeException(errorMessage));
        }
    }
}
