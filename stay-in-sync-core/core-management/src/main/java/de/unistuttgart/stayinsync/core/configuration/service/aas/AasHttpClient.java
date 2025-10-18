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

/**
 * HTTP client for interacting with AAS (Asset Administration Shell) REST APIs.
 * Provides utility methods for sending GET and write (POST, PUT, PATCH, DELETE) JSON requests
 * with logging and safe response body truncation.
 */
@ApplicationScoped
public class AasHttpClient {

    @Inject
    Vertx vertx;

    private WebClient webClient;

    /**
     * Initializes the Vert.x WebClient instance after dependency injection.
     * This method is automatically called after the bean construction.
     */
    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
    }

    /**
     * Sends an HTTP GET request to the specified URL and expects a JSON response.
     * Adds the "Accept: application/json" header if not already present and logs the request and response.
     *
     * @param url The absolute URL to send the GET request to.
     * @param headers Optional map of HTTP headers to include in the request.
     * @return A Uni emitting the HTTP response containing a JSON body.
     */
    public Uni<HttpResponse<Buffer>> getJson(String url, Map<String, String> headers) {
        Log.infof("HTTP GET %s", url);
        var req = webClient.requestAbs(HttpMethod.GET, url);
        if (headers != null) headers.forEach(req.headers()::add);
        if (!req.headers().contains("Accept")) req.putHeader("Accept", "application/json");
        return req.send()
                .invoke(resp -> Log.infof("HTTP GET <- %d %s body=%s", resp.statusCode(), resp.statusMessage(), safeBody(resp)));
    }

    /**
     * Sends an HTTP request with a JSON payload (POST, PUT, PATCH, DELETE supported).
     * Automatically adds "Accept" and "Content-Type" headers if missing and logs request details.
     *
     * @param method The HTTP method to use (e.g., POST, PUT, PATCH, DELETE).
     * @param url The absolute URL to send the request to.
     * @param body The JSON string payload to send.
     * @param headers Optional map of HTTP headers to include in the request.
     * @return A Uni emitting the HTTP response.
     */
    public Uni<HttpResponse<Buffer>> writeJson(HttpMethod method, String url, String body, Map<String, String> headers) {
        Log.infof("HTTP %s %s", method, url);
        var req = webClient.requestAbs(method, url);
        if (headers != null) headers.forEach(req.headers()::add);
        if (!req.headers().contains("Accept")) req.putHeader("Accept", "application/json");
        if (!req.headers().contains("Content-Type")) req.putHeader("Content-Type", "application/json");
        Log.debugf("HTTP %s payload: %s", method, truncate(body));
        return req.sendBuffer(Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(body)))
                .invoke(resp -> Log.infof("HTTP %s <- %d %s body=%s", method, resp.statusCode(), resp.statusMessage(), safeBody(resp)));
    }

    /**
     * Safely extracts and truncates the response body for logging purposes.
     * Prevents exceptions if the response body cannot be read.
     *
     * @param resp The HTTP response from which to extract the body.
     * @return Truncated body string or null if extraction fails.
     */
    private static String safeBody(HttpResponse<Buffer> resp) {
        try {
            String b = resp.bodyAsString();
            return truncate(b);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Truncates a string to a maximum of 500 characters for safe logging.
     * Appends ellipsis if the string exceeds the limit.
     *
     * @param s The string to truncate.
     * @return Truncated string or null if input is null.
     */
    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
