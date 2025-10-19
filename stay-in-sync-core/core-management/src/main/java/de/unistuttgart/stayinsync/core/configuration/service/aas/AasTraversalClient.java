package de.unistuttgart.stayinsync.core.configuration.service.aas;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for traversing and interacting with Asset Administration Shell (AAS) REST APIs.
 * Provides methods for managing shells, submodels, and submodel elements including
 * CRUD operations and reference management.
 */
@ApplicationScoped
public class AasTraversalClient {

    @Inject
    AasHttpClient http;

    /**
     * Retrieves the AAS shell for the given AAS ID.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param aasId The AAS ID to fetch.
     * @param headers The HTTP headers to include in the request.
     * @return Uni emitting the HTTP response containing the AAS shell.
     */
    public Uni<HttpResponse<Buffer>> getShell(String baseUrl, String aasId, Map<String, String> headers) {
        return http.getJson(baseUrl + "/shells/" + encode(aasId), headers);
    }

    /**
     * Lists all submodels associated with a specific AAS.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param aasId The AAS ID.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response with submodel references.
     */
    public Uni<HttpResponse<Buffer>> listSubmodels(String baseUrl, String aasId, Map<String, String> headers) {
        return http.getJson(baseUrl + "/shells/" + encode(aasId) + "/submodel-refs", headers);
    }

    /**
     * Lists submodel elements for a given submodel, with optional depth and parent path parameters.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID.
     * @param depth The desired traversal depth ("core" or "deep").
     * @param parentPath Optional parent path for hierarchical traversal.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response containing submodel elements.
     */
    public Uni<HttpResponse<Buffer>> listElements(String baseUrl, String submodelId, String depth, String parentPath, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements";
        if (parentPath != null && !parentPath.isBlank()) {
            url += "/" + encodePathSegments(parentPath);
        }
        String level = (depth != null && depth.equalsIgnoreCase("all")) ? "deep" : "core";
        url += (url.contains("?") ? "&" : "?") + "level=" + encode(level);
        return http.getJson(url, headers);
    }

    /**
     * Creates a new submodel under the given AAS base URL.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param body JSON representation of the submodel to create.
     * @param headers HTTP headers for the request.
     * @return Uni emitting the HTTP response after creation.
     */
    public Uni<HttpResponse<Buffer>> createSubmodel(String baseUrl, String body, Map<String, String> headers) {
        return http.writeJson(HttpMethod.POST, baseUrl + "/submodels", body, headers);
    }

    /**
     * Retrieves a specific submodel by its ID.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response containing the submodel.
     */
    public Uni<HttpResponse<Buffer>> getSubmodel(String baseUrl, String submodelId, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId);
        return http.getJson(url, headers);
    }


    /**
     * Creates a new element within a specified submodel or submodel path.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID.
     * @param parentPath Optional parent element path.
     * @param body JSON representation of the element to create.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response of the creation request.
     */
    public Uni<HttpResponse<Buffer>> createElement(String baseUrl, String submodelId, String parentPath, String body, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements";
        if (parentPath != null && !parentPath.isBlank()) {
            url += "/" + encodePathSegments(parentPath);
        }
        return http.writeJson(HttpMethod.POST, url, body, headers);
    }

    /**
     * Updates the value of a specific AAS submodel element.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID.
     * @param path Path to the element.
     * @param body JSON string with the new value.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response of the PATCH operation.
     */
    public Uni<HttpResponse<Buffer>> patchElementValue(String baseUrl, String submodelId, String path, String body, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements/" + encodePathSegments(path) + "/$value";
        return http.writeJson(HttpMethod.PATCH, url, body, headers);
    }

    /**
     * Deletes a submodel by its ID.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID to delete.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response of the delete operation.
     */
    public Uni<HttpResponse<Buffer>> deleteSubmodel(String baseUrl, String submodelId, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId);
        return http.writeJson(HttpMethod.DELETE, url, "", headers);
    }

    /**
     * Deletes a submodel element by its path within a given submodel.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID.
     * @param path The path of the element to delete.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response of the delete operation.
     */
    public Uni<HttpResponse<Buffer>> deleteElement(String baseUrl, String submodelId, String path, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements/" + encodePathSegments(path);
        return http.writeJson(HttpMethod.DELETE, url, "", headers);
    }

    /**
     * Updates or replaces a submodel by its ID.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID.
     * @param body JSON string with the updated submodel data.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response.
     */
    public Uni<HttpResponse<Buffer>> putSubmodel(String baseUrl, String submodelId, String body, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId);
        return http.writeJson(HttpMethod.PUT, url, body, headers);
    }

    /**
     * Updates or replaces a submodel element by its path.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID.
     * @param path The path of the element.
     * @param body JSON string representing the updated element.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response.
     */
    public Uni<HttpResponse<Buffer>> putElement(String baseUrl, String submodelId, String path, String body, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements/" + encodePathSegments(path);
        return http.writeJson(HttpMethod.PUT, url, body, headers);
    }

    /**
     * Adds a submodel reference to an AAS shell.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param aasId The AAS ID.
     * @param submodelId The submodel ID to reference.
     * @param headers HTTP headers for the request.
     * @return Uni emitting the HTTP response of the POST operation.
     */
    public Uni<HttpResponse<Buffer>> addSubmodelReferenceToShell(String baseUrl, String aasId, String submodelId, Map<String, String> headers) {
        String url = baseUrl + "/shells/" + encode(aasId) + "/submodel-refs";
        String idType = inferIdType(submodelId);
        String body = "{\"type\":\"ModelReference\",\"keys\":[{\"type\":\"Submodel\",\"idType\":\"" + idType + "\",\"value\":\"" + submodelId + "\"}]}";
        return http.writeJson(HttpMethod.POST, url, body, headers);
    }

    /**
     * Removes a submodel reference from an AAS shell using the submodel ID.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param aasId The AAS ID.
     * @param submodelId The submodel ID to remove.
     * @param headers HTTP headers for the request.
     * @return Uni emitting the HTTP response of the DELETE operation.
     */
    public Uni<HttpResponse<Buffer>> removeSubmodelReferenceFromShell(String baseUrl, String aasId, String submodelId, Map<String, String> headers) {
        String url = baseUrl + "/shells/" + encode(aasId) + "/submodel-refs/" + encode(submodelId);
        return http.writeJson(HttpMethod.DELETE, url, "", headers);
    }

    /**
     * Retrieves all submodel references associated with an AAS shell.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param aasId The AAS ID.
     * @param headers HTTP headers for the request.
     * @return Uni emitting the HTTP response containing submodel references.
     */
    public Uni<HttpResponse<Buffer>> listSubmodelReferences(String baseUrl, String aasId, Map<String, String> headers) {
        String url = baseUrl + "/shells/" + encode(aasId) + "/submodel-refs";
        return http.getJson(url, headers);
    }

    /**
     * Removes a submodel reference from a shell by its index position.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param aasId The AAS ID.
     * @param index The index of the reference to remove.
     * @param headers HTTP headers for the request.
     * @return Uni emitting the HTTP response of the DELETE operation.
     */
    public Uni<HttpResponse<Buffer>> removeSubmodelReferenceFromShellByIndex(String baseUrl, String aasId, int index, Map<String, String> headers) {
        String url = baseUrl + "/shells/" + encode(aasId) + "/submodel-refs/" + index;
        return http.writeJson(HttpMethod.DELETE, url, "", headers);
    }

    /**
     * URL-encodes a given string using UTF-8 according to RFC 3986.
     * Replaces '+' with '%20' to ensure proper space encoding.
     *
     * @param s The string to encode.
     * @return The encoded string.
     */
    private String encode(String s) {
        String enc = java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        return enc.replace("+", "%20");
    }

    /**
     * Encodes submodel element path segments, joining them using dots instead of slashes.
     *
     * @param path The raw element path.
     * @return The encoded, dot-separated path string.
     */
    private String encodePathSegments(String path) {
        return Arrays.stream(path.split("\\."))
                .map(this::encode)
                .collect(Collectors.joining("."));
    }

    /**
     * Infers the ID type (IRI or Custom) based on the given identifier format.
     *
     * @param id The identifier to analyze.
     * @return "IRI" if the ID starts with 'http', 'https', or 'urn'; otherwise "Custom".
     */
    private String inferIdType(String id) {
        if (id == null) return "Custom";
        String lower = id.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("urn:")) {
            return "IRI";
        }
        return "Custom";
    }

    /**
     * Retrieves a specific submodel element with deep-level traversal.
     *
     * @param baseUrl The base URL of the AAS API.
     * @param submodelId The submodel ID.
     * @param path The element path within the submodel.
     * @param headers HTTP headers to include in the request.
     * @return Uni emitting the HTTP response containing the element data.
     */
    public Uni<HttpResponse<Buffer>> getElement(String baseUrl, String submodelId, String path, Map<String, String> headers) {
        String url = baseUrl + "/submodels/" + encode(submodelId) + "/submodel-elements/" + encodePathSegments(path) + "?level=deep";
        return http.getJson(url, headers);
    }

}


