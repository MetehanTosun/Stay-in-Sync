package de.unistuttgart.stayinsync.core.configuration.service.aas;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class AasTraversalClient {

    @Inject
    AasHttpClient http;

    public Uni<HttpResponse<Buffer>> getShell(String baseUrl, String aasId, Map<String, String> headers) {
        return http.getJson(baseUrl + "/shells/" + encode(aasId), headers);
    }

    public Uni<HttpResponse<Buffer>> listSubmodels(String baseUrl, String aasId, Map<String, String> headers) {
        return http.getJson(baseUrl + "/shells/" + encode(aasId) + "/submodel-refs", headers);
    }

    public Uni<HttpResponse<Buffer>> listElements(String baseUrl, String submodelId, String depth, String parentPath, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements";
        if (parentPath != null && !parentPath.isBlank()) {
            url += "/" + parentPath;
        }
        String level = (depth != null && depth.equalsIgnoreCase("all")) ? "deep" : "core";
        url += (url.contains("?") ? "&" : "?") + "level=" + encode(level);
        return http.getJson(url, headers);
    }

    public Uni<HttpResponse<Buffer>> createSubmodel(String baseUrl, String body, Map<String, String> headers) {
        return http.writeJson(HttpMethod.POST, baseUrl + "/submodels", body, headers);
    }

    public Uni<HttpResponse<Buffer>> getSubmodel(String baseUrl, String submodelId, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId);
        return http.getJson(url, headers);
    }

    public Uni<HttpResponse<Buffer>> createElement(String baseUrl, String submodelId, String parentPath, String body, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements";
        if (parentPath != null && !parentPath.isBlank()) {
            url += "/" + parentPath;
        }
        return http.writeJson(HttpMethod.POST, url, body, headers);
    }

    public Uni<HttpResponse<Buffer>> patchElementValue(String baseUrl, String submodelId, String path, String body, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements/" + path + "/$value";
        return http.writeJson(HttpMethod.PATCH, url, body, headers);
    }

    public Uni<HttpResponse<Buffer>> deleteSubmodel(String baseUrl, String submodelId, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId);
        return http.writeJson(HttpMethod.DELETE, url, "", headers);
    }

    public Uni<HttpResponse<Buffer>> deleteElement(String baseUrl, String submodelId, String path, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements/" + path;
        return http.writeJson(HttpMethod.DELETE, url, "", headers);
    }

    public Uni<HttpResponse<Buffer>> putSubmodel(String baseUrl, String submodelId, String body, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId);
        return http.writeJson(HttpMethod.PUT, url, body, headers);
    }

    public Uni<HttpResponse<Buffer>> putElement(String baseUrl, String submodelId, String path, String body, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements/" + path;
        return http.writeJson(HttpMethod.PUT, url, body, headers);
    }

    public Uni<HttpResponse<Buffer>> addSubmodelReferenceToShell(String baseUrl, String aasId, String submodelId, Map<String, String> headers) {
        String url = baseUrl + "/shells/" + encode(aasId) + "/submodel-refs";
        String body = "{\"type\":\"ModelReference\",\"keys\":[{\"type\":\"Submodel\",\"value\":\"" + submodelId + "\"}]}";
        return http.writeJson(HttpMethod.POST, url, body, headers);
    }

    public Uni<HttpResponse<Buffer>> removeSubmodelReferenceFromShell(String baseUrl, String aasId, String submodelId, Map<String, String> headers) {
        String url = baseUrl + "/shells/" + encode(aasId) + "/submodel-refs/" + encode(submodelId);
        return http.writeJson(HttpMethod.DELETE, url, "", headers);
    }

    public Uni<HttpResponse<Buffer>> listSubmodelReferences(String baseUrl, String aasId, Map<String, String> headers) {
        String url = baseUrl + "/shells/" + encode(aasId) + "/submodel-refs";
        return http.getJson(url, headers);
    }

    public Uni<HttpResponse<Buffer>> removeSubmodelReferenceFromShellByIndex(String baseUrl, String aasId, int index, Map<String, String> headers) {
        String url = baseUrl + "/shells/" + encode(aasId) + "/submodel-refs/" + index;
        return http.writeJson(HttpMethod.DELETE, url, "", headers);
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}


