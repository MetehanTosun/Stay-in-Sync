package de.unistuttgart.stayinsync.core.configuration.service.aas;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class AasHttpClient {

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
    }

    public Uni<HttpResponse<Buffer>> getJson(String url, Map<String, String> headers) {
        Log.infof("HTTP GET %s", url);
        var req = webClient.requestAbs(HttpMethod.GET, url);
        if (headers != null) headers.forEach(req.headers()::add);
        if (!req.headers().contains("Accept")) req.putHeader("Accept", "application/json");
        return req.send();
    }

    public Uni<HttpResponse<Buffer>> writeJson(HttpMethod method, String url, String body, Map<String, String> headers) {
        Log.infof("HTTP %s %s", method, url);
        var req = webClient.requestAbs(method, url);
        if (headers != null) headers.forEach(req.headers()::add);
        if (!req.headers().contains("Accept")) req.putHeader("Accept", "application/json");
        if (!req.headers().contains("Content-Type")) req.putHeader("Content-Type", "application/json");
        return req.sendBuffer(Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(body)));
    }
}


