package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.enterprise.context.ApplicationScoped;


/**
 * Offers methods to build CRUD requests excluding DELETE, as well as the method executeRequest(HttpRequest<Buffer> request),
 * that executes these requests and returns the polled data.
 */
@ApplicationScoped
public class RestClient {

    /**
     * Executes a prebuilt request and returns a JsonObject when poll is done.
     *
     * @param request is the prebuild parameterised HttpRequest that is executed
     * @return Uni<JsonObject> form which the JsonObject can be retrieved
     */
    public Uni<HttpResponse<Buffer>> executeRequest(final HttpRequest<Buffer> request) {
        return request.send()
                .onItem().transform(response -> {
                    Log.info(response.bodyAsString());
                    return response;
                });
    }
 }
