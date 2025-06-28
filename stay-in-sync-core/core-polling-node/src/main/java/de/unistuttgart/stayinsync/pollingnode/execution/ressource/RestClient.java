package java.de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;

import java.time.Duration;

/**
 * Offers methods to poll JsonObjects of any RestAPI.
 */
@ApplicationScoped
public class RestClient {

    private final WebClient webClient;

    /** Constructs RestClient with single WebClient to increase polling efficiency by removing that step from that process.
     * @param vertx used to manage the WebClient's event loop, connection pooling, and thread model.
     *              Must be provided by the Quarkus dependency injection to ensure proper lifecycle management.
     */
    public RestClient(final Vertx vertx) {
        this.webClient = WebClient.create(vertx, new WebClientOptions()
                .setConnectTimeout((int) Duration.ofSeconds(10).toMillis())
                .setIdleTimeout((int) Duration.ofSeconds(30).toMillis())
                .setKeepAlive(true)
                .setMaxPoolSize(20));

    }

    /**
     * Polls a JsonObject and returns it asynchronous by using mutiny framework.
     * @param apiAddress used to call the RestAPI and get its data.
     * @return JsonObject as soon as polling is finished in Uni form.
     */
    public Uni<JsonObject> getJsonOfApi(String apiAddress) {
        Log.debugf("Json Polling started for Api: %s", apiAddress);

        return webClient.getAbs(apiAddress)
                .send()
                .onItem().transform(Unchecked.function(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonData = response.bodyAsJsonObject();
                        if (jsonData == null) {
                            throw new RuntimeException("Response is invalid Json: " + apiAddress);
                        }
                        return jsonData;
                    } else {
                        throw new RuntimeException(
                                String.format("Http error %d for %s: %s",
                                        response.statusCode(), apiAddress, response.statusMessage())
                        );
                    }
                }))
                .onItem().invoke(() ->
                        Log.debugf("Json polled successfully: %s", apiAddress)
                )
                .onFailure().invoke(throwable ->
                        Log.errorf(throwable, "Error during Json poll of: %s", apiAddress)
                );
    }

    /**
     * Shuts down the WebClient and releases associated resources.
     * This method is automatically invoked by the container during application shutdown
     * to ensure all underlying network connections and thread resources are properly cleaned up.
     */
    @jakarta.annotation.PreDestroy
    public void cleanup() {
        if (webClient != null) {
            webClient.close();
            Log.debug("WebClient was closed correctly");
        }
    }
}
