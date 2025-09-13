package de.unistuttgart.stayinsync.core.syncnode.syncjob;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WebClientProvider {

    private final WebClient client;

    @Inject
    public WebClientProvider(Vertx vertx){
        WebClientOptions options = new WebClientOptions()
                .setTrustAll(true) // TODO: production code should not trust all
                .setSsl(true)
                .setConnectTimeout(5000);
        this.client = WebClient.create(vertx, options);
    }

    public WebClient getClient(){
        return client;
    }
}
