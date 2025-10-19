package de.unistuttgart.stayinsync.core.configuration.rest.aas.source;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasTestResultDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelElementNodeDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelSummaryDTO;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.service.aas.SourceSystemAasService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.nio.file.Files;


/**
 * REST resource for handling AAS (Asset Administration Shell) operations related to Source Systems.
 * Provides endpoints for listing, creating, updating, and deleting AAS submodels and elements.
 * Supports both LIVE and SNAPSHOT data sources.
 */
@Path("/api/config/source-system/{sourceSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class SourceAasResource {


    @Inject
    SourceSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    AasStructureSnapshotService snapshotService;

    @Inject
    de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder headerBuilder;

    /**
     * Retrieves submodel elements for a given Source System and submodel.
     * Supports both LIVE and SNAPSHOT sources and handles nested element flattening for lists and collections.
     * @param sourceSystemId ID of the source system.
     * @param smId Submodel ID.
     * @param depth Depth mode (shallow or all).
     * @param parentPath Optional path of parent element.
     * @param source Data source type (LIVE or SNAPSHOT).
     * @return A reactive Uni<Response> containing the elements.
     */
    @GET
    @Path("/submodels/{smId}/elements")
    public Uni<Response> listElements(@PathParam("sourceSystemId") Long sourceSystemId,
                                 @PathParam("smId") String smId,
                                 @QueryParam("depth") @DefaultValue("shallow") String depth,
                                 @QueryParam("parentPath") String parentPath,
                                 @QueryParam("source") @DefaultValue("SNAPSHOT") String source) {
        Log.infof("Source listElements ENTRY: sourceSystemId=%d smId=%s depth=%s parentPath=%s source=%s", 
                  sourceSystemId, smId, depth, parentPath, source);
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        if ("LIVE".equalsIgnoreCase(source)) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            final String apiUrl = ss.apiUrl;
            final java.util.Map<String,String> headersLive = headers;
            return traversal.listElements(apiUrl, smId, depth, parentPath, headersLive).onItem().transformToUni(resp -> {
                int sc = resp.statusCode();
                if (sc >= 200 && sc < 300) {
                    String body = resp.bodyAsString();
                    Log.infof("Source listElements MAIN: status=%d depth=%s parentPath=%s", sc, depth, parentPath);
                    
                    /* SUCCESS PATH: Check if server returned parent object instead of children array */
                    if (parentPath != null && !parentPath.isBlank() && body != null && !body.isBlank()) {
                        Response r = buildChildrenResponseFromParentBody(body, parentPath, false);
                        if (r != null) return Uni.createFrom().item(r);
                    }
                    
                    return Uni.createFrom().item(Response.ok(body).build());
                }
                /* Fallback for nested collections returning 404/400 on parent path */
                if ((sc == 404 || sc == 400) && parentPath != null && !parentPath.isBlank()) {
                        Log.infof("Source listElements 404/400-fallback: trying deep fetch for parentPath=%s", parentPath);
                    return buildDeepChildrenFallbackResponseReactive(apiUrl, smId, parentPath, headersLive);
                }
                
                Log.infof("Source listElements MAIN: final return status=%d", sc);
                return Uni.createFrom().item(Response.status(sc).entity(resp.bodyAsString()).build());
            }).onFailure().recoverWithItem(ex -> {
                Log.errorf(ex, "Source listElements LIVE: Unexpected error");
                return Response.serverError().entity(ex.getMessage()).build();
            });
        }
        
         
        if ("SNAPSHOT".equalsIgnoreCase(source) && parentPath != null && !parentPath.isBlank()) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            final String apiUrl = ss.apiUrl;
            final java.util.Map<String,String> headersSnapshot = headers;
            /* Convert parentPath from slash notation to dot notation for BaSyx compatibility */
            final String dotNotationPath = parentPath.replace("/", ".");
            Log.infof("Source listElements SNAPSHOT: Converting parentPath from slash to dot notation: %s -> %s", parentPath, dotNotationPath);
            return traversal.listElements(apiUrl, smId, depth, dotNotationPath, headersSnapshot).map(resp -> {
                int sc = resp.statusCode();
                if (sc >= 200 && sc < 300) {
                    String body = resp.bodyAsString();
                    Log.infof("Source listElements SNAPSHOT: status=%d depth=%s parentPath=%s", sc, depth, parentPath);
                    /* SUCCESS PATH: Check if server returned parent object instead of children array */
                    if (parentPath != null && !parentPath.isBlank() && body != null && !body.isBlank()) {
                        Response r = buildChildrenResponseFromParentBody(body, parentPath, true);
                        if (r != null) return r;
                    }
                    return Response.ok(body).build();
                }
                
                Log.infof("Source listElements SNAPSHOT: final return status=%d", sc);
                return Response.status(sc).entity(resp.bodyAsString()).build();
            });
        }
        
        String normalizedSmId = normalizeSubmodelId(smId);
        var submodel = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", sourceSystemId, normalizedSmId).firstResult();
        if (submodel == null) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            Log.infof("List elements LIVE (no snapshot): apiUrl=%s smId=%s depth=%s parentPath=%s", ss.apiUrl, smId, depth, parentPath);
            Log.debugf("LIVE headers: %s", headers);
            return traversal.listElements(ss.apiUrl, smId, depth, parentPath, headers).map(resp -> {
                int sc = resp.statusCode();
                Log.infof("List elements upstream status=%d msg=%s", sc, resp.statusMessage());
                if (sc >= 200 && sc < 300) {
                    String body = resp.bodyAsString();
                    
                    /* Apply same SubmodelElementList flattening logic as in main listElements */
                    if (parentPath != null && !parentPath.isBlank() && body != null && !body.isBlank()) {
                        try {
                            if (body.trim().startsWith("{")) {
                                io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(body);
                                String modelType = obj.getString("modelType");
                                if ("SubmodelElementList".equalsIgnoreCase(modelType)) {
                                    var v = obj.getValue("value");
                                    if (v instanceof io.vertx.core.json.JsonArray directChildren) {
                                        io.vertx.core.json.JsonArray flattened = new io.vertx.core.json.JsonArray();
                                        for (int i = 0; i < directChildren.size(); i++) {
                                            var listItem = directChildren.getJsonObject(i);
                                            if (listItem == null) continue;
                                            String idxId = listItem.getString("idShort");
                                            if (idxId == null || idxId.isBlank()) idxId = Integer.toString(i);
                                            Object itemVal = listItem.getValue("value");
                                            if (itemVal instanceof io.vertx.core.json.JsonArray carr) {
                                                for (int j = 0; j < carr.size(); j++) {
                                                    var child = carr.getJsonObject(j);
                                                    if (child == null) continue;
                                                    String cid = child.getString("idShort");
                                                    if (cid != null && !cid.isBlank()) child.put("idShortPath", parentPath + "/" + idxId + "/" + cid);
                                                    flattened.add(child);
                                                }
                                            }
                                        }
                                        if (!flattened.isEmpty()) return Response.ok(flattened.encode()).build();
                                    }
                                }
                            }
                        } catch (Exception ignore) {}
                    }
                    
                    return Response.ok(body).build();
                }
                aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
                return null;  
            });
        }
        java.util.List<AasElementLite> elements;
        if ("all".equalsIgnoreCase(depth)) {
            if (parentPath == null || parentPath.isBlank()) {
                elements = AasElementLite.list("submodelLite.id", submodel.id);
            } else {
                String prefix = parentPath.endsWith("/") ? parentPath : parentPath + "/";
                elements = AasElementLite.list("submodelLite.id = ?1 and (idShortPath = ?2 or idShortPath like ?3)", submodel.id, parentPath, prefix + "%");
            }
        } else {
            if (parentPath == null || parentPath.isBlank()) {
                elements = AasElementLite.list("submodelLite.id = ?1 and parentPath is null", submodel.id);
            } else {
                elements = AasElementLite.list("submodelLite.id = ?1 and parentPath = ?2", submodel.id, parentPath);
            }
        }
        var list = elements.stream().map(e -> new SubmodelElementNodeDTO(
                e.modelType,
                e.idShort,
                e.idShortPath,
                e.hasChildren,
                e.valueType,
                e.semanticId,
                e.isReference,
                e.referenceTargetType,
                e.referenceKeys,
                e.targetSubmodelId,
                e.typeValueListElement,
                e.orderRelevant,
                null,
                null,
                e.id
        )).toList();
        if (!list.isEmpty()) {
            return Uni.createFrom().item(Response.ok().entity(list).build());
        }
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
        Log.infof("List elements LIVE (fallback): apiUrl=%s smId=%s depth=%s parentPath=%s", ss.apiUrl, smId, depth, parentPath);
        Log.debugf("LIVE headers: %s", headers);
        return traversal.listElements(ss.apiUrl, smId, depth, parentPath, headers).map(resp -> {
            int sc = resp.statusCode();
            Log.infof("List elements upstream status=%d msg=%s", sc, resp.statusMessage());
            if (sc >= 200 && sc < 300) {
                String body = resp.bodyAsString();
                
                /* Apply same SubmodelElementList flattening logic as in main listElements */
                if (parentPath != null && !parentPath.isBlank() && body != null && !body.isBlank()) {
                    try {
                        if (body.trim().startsWith("{")) {
                            io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(body);
                            String modelType = obj.getString("modelType");
                            if ("SubmodelElementList".equalsIgnoreCase(modelType)) {
                                var v = obj.getValue("value");
                                if (v instanceof io.vertx.core.json.JsonArray directChildren) {
                                    io.vertx.core.json.JsonArray flattened = new io.vertx.core.json.JsonArray();
                                    for (int i = 0; i < directChildren.size(); i++) {
                                        var listItem = directChildren.getJsonObject(i);
                                        if (listItem == null) continue;
                                        String idxId = listItem.getString("idShort");
                                        if (idxId == null || idxId.isBlank()) idxId = Integer.toString(i);
                                        Object itemVal = listItem.getValue("value");
                                        if (itemVal instanceof io.vertx.core.json.JsonArray carr) {
                                            for (int j = 0; j < carr.size(); j++) {
                                                var child = carr.getJsonObject(j);
                                                if (child == null) continue;
                                                String cid = child.getString("idShort");
                                                if (cid != null && !cid.isBlank()) child.put("idShortPath", parentPath + "/" + idxId + "/" + cid);
                                                flattened.add(child);
                                            }
                                        }
                                    }
                                    if (!flattened.isEmpty()) return Response.ok(flattened.encode()).build();
                                }
                            }
                        }
                    } catch (Exception ignore) {}
                }
                
                return Response.ok(body).build();
            }
            aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
            return null; 
        });
    }

    /**
     * Creates a new element within a specified submodel for the given Source System.
     * Automatically adjusts the parent path based on BaSyx structure types.
     * @param sourceSystemId ID of the source system.
     * @param smId Submodel ID.
     * @param parentPath Optional parent path within the submodel.
     * @param body JSON payload of the new element.
     * @return HTTP Response with creation result.
     */
    @POST
    @Path("/submodels/{smId}/elements")
    public Response createElement(@PathParam("sourceSystemId") Long sourceSystemId,
                                  @PathParam("smId") String smId,
                                  @QueryParam("parentPath") String parentPath,
                                  String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("Create element LIVE: apiUrl=%s smId=%s parentPath=%s", ss.apiUrl, smId, parentPath);
        Log.debugf("WRITE headers: %s body=%s", headers, body);
        String effectiveParentPath = (parentPath != null && !parentPath.isBlank()) ? parentPath : null;
        /* Adjust parent path for types that require sub-paths in BaSyx (collections/lists: /value, entity: /statements) */
        if (effectiveParentPath != null) {
            try {
                String normalizedSmId = normalizeSubmodelId(smId);
                var parentResp = traversal.getElement(ss.apiUrl, normalizedSmId, parentPath, headers).await().indefinitely();
                if (parentResp != null && parentResp.statusCode() >= 200 && parentResp.statusCode() < 300) {
                    String pb = parentResp.bodyAsString();
                    io.vertx.core.json.JsonObject pobj = pb != null && pb.trim().startsWith("{")
                            ? new io.vertx.core.json.JsonObject(pb)
                            : null;
                    if (pobj != null) {
                        String mt = pobj.getString("modelType");
                        if (mt != null) {
                            if ("SubmodelElementCollection".equalsIgnoreCase(mt) || "SubmodelElementList".equalsIgnoreCase(mt)) {
                                effectiveParentPath = parentPath.endsWith("/value") ? parentPath : parentPath + "/value";
                            } else if ("Entity".equalsIgnoreCase(mt)) {
                                effectiveParentPath = parentPath.endsWith("/statements") ? parentPath : parentPath + "/statements";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.warnf("Could not inspect parent element for path suffix resolution: %s", e.getMessage());
            }
        }
        /* Use raw smId (Base64URL) for upstream like Target System */
        var resp = traversal.createElement(ss.apiUrl, smId, effectiveParentPath, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("Create element upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            String normalizedSmId = normalizeSubmodelId(smId);
            snapshotService.applyElementCreate(sourceSystemId, normalizedSmId, parentPath, resp.bodyAsString());
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        return null;  
    }
    /**
     * Updates (replaces) an existing AAS element in a submodel.
     * @param sourceSystemId ID of the source system.
     * @param smId Submodel ID.
     * @param path Path of the element within the submodel.
     * @param body JSON payload containing updated element data.
     * @return HTTP Response with the update result.
     */
    @PUT
    @Path("/submodels/{smId}/elements/{path:.+}")
    public Response putElement(@PathParam("sourceSystemId") Long sourceSystemId,
                               @PathParam("smId") String smId,
                               @PathParam("path") String path,
                               String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("PUT element LIVE: apiUrl=%s smId=%s path=%s", ss.apiUrl, smId, path);
        var resp = traversal.putElement(ss.apiUrl, smId, path, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("PUT element upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            return Response.ok(resp.bodyAsString()).build();
        }
        aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        return null;   
    }
    /**
     * Deletes a specific element from a submodel for the given Source System.
     * Also updates the snapshot cache to reflect the deletion.
     * @param sourceSystemId ID of the source system.
     * @param smId Submodel ID.
     * @param path Path of the element to delete.
     * @return HTTP Response with deletion status.
     */
    @DELETE
    @Path("/submodels/{smId}/elements/{path:.+}")
    public Response deleteElement(@PathParam("sourceSystemId") Long sourceSystemId,
                                  @PathParam("smId") String smId,
                                  @PathParam("path") String path) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("DELETE element LIVE: apiUrl=%s smId=%s path=%s", ss.apiUrl, smId, path);
        var resp = traversal.deleteElement(ss.apiUrl, smId, path, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("DELETE element upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            String normalizedSmId = normalizeSubmodelId(smId);
            snapshotService.applyElementDelete(sourceSystemId, normalizedSmId, path);
            return Response.noContent().build();
        }
        aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        return null; 
    }

    /**
     * Safely retrieves the HTTP response body while truncating long outputs for logging.
     * @param resp The HTTP response object.
     * @return Truncated response body or null if unavailable.
     */
    private String safeBody(HttpResponse<Buffer> resp) {
        try {
            String b = resp.bodyAsString();
            if (b == null) return null;
            return b.length() > 500 ? b.substring(0, 500) + "..." : b;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Attempts to resolve an element deeply via recursive traversal when direct retrieval fails.
     * Used for nested or indirectly accessible elements.
     * @param apiUrl Base API URL.
     * @param smId Submodel ID.
     * @param idShortPath Path to the element.
     * @param headers HTTP headers for the request.
     * @return JSON object of the resolved element or null if not found.
     */
    private io.vertx.core.json.JsonObject resolveElementDeep(String apiUrl, String smId, String idShortPath, java.util.Map<String,String> headers) {
        try {
            /* Try direct GET first (deep) */
            var direct = traversal.getElement(apiUrl, smId, idShortPath, headers).await().indefinitely();
            if (direct.statusCode() >= 200 && direct.statusCode() < 300) {
                String body = direct.bodyAsString();
                if (body != null && !body.isBlank()) {
                    if (body.trim().startsWith("{")) return new io.vertx.core.json.JsonObject(body);
                    /* if wrapped */
                    return new io.vertx.core.json.JsonObject().put("result", new io.vertx.core.json.JsonArray(body));
                }
            }
        } catch (Exception ignore) {}
        try {
            /* Fallback: list all (deep) and recursively pick matching idShortPath */
            var all = traversal.listElements(apiUrl, smId, "all", null, headers).await().indefinitely();
            if (all.statusCode() >= 200 && all.statusCode() < 300) {
                String body = all.bodyAsString();
                io.vertx.core.json.JsonArray root = body != null && body.trim().startsWith("{")
                        ? new io.vertx.core.json.JsonObject(body).getJsonArray("result", new io.vertx.core.json.JsonArray())
                        : new io.vertx.core.json.JsonArray(body);

                io.vertx.core.json.JsonObject found = findElementByPathRecursive(root, idShortPath);
                if (found != null) return found;
            }
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * Builds a children array response from a parent element's JSON body.
     * Handles special structures like SubmodelElementList and Entity.
     * @param body Parent JSON body.
     * @param parentPath Parent element path.
     * @param snapshotMode Whether the mode is snapshot (true) or live (false).
     * @return HTTP Response containing the processed children or null if parsing fails.
     */
    private Response buildChildrenResponseFromParentBody(String body, String parentPath, boolean snapshotMode) {
        try {
            if (body.trim().startsWith("{")) {
                io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(body);
                String modelType = obj.getString("modelType");
                io.vertx.core.json.JsonArray directChildren = null;
                if ("SubmodelElementCollection".equalsIgnoreCase(modelType) || "SubmodelElementList".equalsIgnoreCase(modelType)) {
                    var v = obj.getValue("value");
                    if (v instanceof io.vertx.core.json.JsonArray arr) directChildren = arr;
                } else if ("Entity".equalsIgnoreCase(modelType)) {
                    var st = obj.getValue("statements");
                    if (st instanceof io.vertx.core.json.JsonArray arr) directChildren = arr;
                }
                if (directChildren != null) {
                    Log.infof("Source listElements: parent modelType=%s children=%d parentPath=%s", modelType, directChildren.size(), parentPath);
                    /* Special handling for SubmodelElementList flattening */
                    if ("SubmodelElementList".equalsIgnoreCase(modelType)) {
                        io.vertx.core.json.JsonArray flattened = new io.vertx.core.json.JsonArray();
                        for (int i = 0; i < directChildren.size(); i++) {
                            var listItem = directChildren.getJsonObject(i);
                            if (listItem == null) continue;
                            String idxId = listItem.getString("idShort");
                            if (idxId == null || idxId.isBlank()) idxId = Integer.toString(i);
                            Object itemVal = listItem.getValue("value");
                            if (itemVal instanceof io.vertx.core.json.JsonArray carr) {
                                for (int j = 0; j < carr.size(); j++) {
                                    var child = carr.getJsonObject(j);
                                    if (child == null) continue;
                                    String cid = child.getString("idShort");
                                    if (cid != null && !cid.isBlank()) child.put("idShortPath", parentPath + "/" + idxId + "/" + cid);
                                    flattened.add(child);
                                }
                            } else if (snapshotMode) {
                                if (idxId != null && !idxId.isBlank()) listItem.put("idShortPath", parentPath + "/" + idxId);
                                flattened.add(listItem);
                            }
                        }
                        if (!flattened.isEmpty()) return Response.ok(flattened.encode()).build();
                    }
                    /* Regular children processing */
                    io.vertx.core.json.JsonArray out = new io.vertx.core.json.JsonArray();
                    for (int i = 0; i < directChildren.size(); i++) {
                        var el = directChildren.getJsonObject(i);
                        if (el == null) continue;
                        String idShort = el.getString("idShort");
                        if ((idShort == null || idShort.isBlank()) && "SubmodelElementList".equalsIgnoreCase(modelType)) {
                            idShort = snapshotMode ? ("item_" + i) : Integer.toString(i);
                            el.put("idShort", idShort);
                        }
                        if (idShort != null && !idShort.isBlank()) el.put("idShortPath", parentPath + "/" + idShort);
                        out.add(el);
                    }
                    return Response.ok(out.encode()).build();
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * Reactive fallback: When a nested collection returns 404/400, fetch entire submodel with level=deep,
     * then filter to direct children of the given parent path.
     * @param apiUrl Base URL for traversal client.
     * @param smId Submodel ID.
     * @param parentPath Parent element path.
     * @param headers HTTP headers.
     * @return Uni<Response> with filtered child elements.
     */
    private Uni<Response> buildDeepChildrenFallbackResponseReactive(String apiUrl, String smId, String parentPath, java.util.Map<String,String> headers) {
        return traversal.listElements(apiUrl, smId, "all", null, headers)
            .onItem().transform(all -> {
                if (all.statusCode() >= 200 && all.statusCode() < 300) {
                    String body = all.bodyAsString();
                    io.vertx.core.json.JsonArray arr = body != null && body.trim().startsWith("{")
                            ? new io.vertx.core.json.JsonObject(body).getJsonArray("result", new io.vertx.core.json.JsonArray())
                            : new io.vertx.core.json.JsonArray(body);
                    String prefix = parentPath.endsWith("/") ? parentPath : parentPath + "/";
                    io.vertx.core.json.JsonArray children = new io.vertx.core.json.JsonArray();
                    for (int i = 0; i < arr.size(); i++) {
                        var el = arr.getJsonObject(i);
                        String p = el.getString("idShortPath", el.getString("idShort"));
                        if (p == null) continue;
                        if (p.equals(parentPath)) continue; /* skip parent itself */
                        if (p.startsWith(prefix)) {
                            String rest = p.substring(prefix.length());
                            if (!rest.contains("/")) {
                                children.add(el);
                            }
                        }
                    }
                    return Response.ok(children.encode()).build();
                }
                Log.infof("Source listElements 404/400-fallback: deep fetch returned status=%d for parentPath=%s", all.statusCode(), parentPath);
                return Response.ok("[]").build();
            })
            .onFailure().recoverWithItem(ex -> {
                Log.infof("Source listElements 404/400-fallback: exception in fallback processing: %s", ex.getMessage());
                Log.infof("Source listElements 404/400-fallback: returning final empty array for parentPath=%s", parentPath);
                return Response.ok("[]").build();
            });
    }

    /**
     * Fallback handler that performs a deep fetch of all elements and extracts children for a given parent path.
     * Used when the parent element returns 404 or 400 in nested structures.
     * @param apiUrl API base URL.
     * @param smId Submodel ID.
     * @param parentPath Parent element path.
     * @param headers HTTP headers.
     * @return HTTP Response with filtered child elements.
     * @deprecated Use buildDeepChildrenFallbackResponseReactive instead to avoid blocking the event loop
     */
    @Deprecated
    private Response buildDeepChildrenFallbackResponse(String apiUrl, String smId, String parentPath, java.util.Map<String,String> headers) {
        try {
            Log.infof("Source listElements 404/400-fallback: trying deep fetch for parentPath=%s", parentPath);
            var all = traversal.listElements(apiUrl, smId, "all", null, headers).await().indefinitely();
            if (all.statusCode() >= 200 && all.statusCode() < 300) {
                String body = all.bodyAsString();
                io.vertx.core.json.JsonArray arr = body != null && body.trim().startsWith("{")
                        ? new io.vertx.core.json.JsonObject(body).getJsonArray("result", new io.vertx.core.json.JsonArray())
                        : new io.vertx.core.json.JsonArray(body);
                String prefix = parentPath.endsWith("/") ? parentPath : parentPath + "/";
                io.vertx.core.json.JsonArray children = new io.vertx.core.json.JsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    var el = arr.getJsonObject(i);
                    String p = el.getString("idShortPath", el.getString("idShort"));
                    if (p == null) continue;
                    if (p.equals(parentPath)) continue; /* skip parent itself */
                    if (p.startsWith(prefix)) {
                        String rest = p.substring(prefix.length());
                        if (!rest.contains("/")) {
                            children.add(el);
                        }
                    }
                }
                return Response.ok(children.encode()).build();
            }
        } catch (Exception ignore) {
            Log.infof("Source listElements 404/400-fallback: exception in fallback processing: %s", ignore.getMessage());
        }
        Log.infof("Source listElements 404/400-fallback: returning final empty array for parentPath=%s", parentPath);
        return Response.ok("[]").build();
    }

    /**
     * Recursively searches for an element by its idShortPath within an array of elements.
     * @param elements JSON array of elements to search through.
     * @param targetPath Path to match.
     * @return The found JSON object or null if not found.
     */
    private io.vertx.core.json.JsonObject findElementByPathRecursive(io.vertx.core.json.JsonArray elements, String targetPath) {
        if (elements == null) return null;
        for (int i = 0; i < elements.size(); i++) {
            var el = elements.getJsonObject(i);
            if (el == null) continue;
            String idShort = el.getString("idShort");
            if (idShort == null || idShort.isBlank()) continue;
            /* Start recursive descent from this element */
            var found = descendAndMatch(el, idShort, targetPath);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Recursively traverses nested element hierarchies to match the target path.
     * @param element Current JSON element being checked.
     * @param currentPath The current hierarchical path of the element.
     * @param targetPath The full path being searched for.
     * @return JSON object if found, otherwise null.
     */
    private io.vertx.core.json.JsonObject descendAndMatch(io.vertx.core.json.JsonObject element, String currentPath, String targetPath) {
        if (targetPath.equals(currentPath)) return element;
        String modelType = element.getString("modelType");
        /* Collections and Lists: values are arrays of child elements */
        if ("SubmodelElementCollection".equalsIgnoreCase(modelType) || "SubmodelElementList".equalsIgnoreCase(modelType)) {
            var value = element.getValue("value");
            if (value instanceof io.vertx.core.json.JsonArray arr) {
                for (int i = 0; i < arr.size(); i++) {
                    var child = arr.getJsonObject(i);
                    if (child == null) continue;
                    String childIdShort = child.getString("idShort");
                    if (childIdShort == null) continue;
                    String childPath = currentPath + "/" + childIdShort;
                    var found = descendAndMatch(child, childPath, targetPath);
                    if (found != null) return found;
                }
            }
        } else if ("Entity".equalsIgnoreCase(modelType)) {
            /* Entities may keep children under 'statements' */
            var statements = element.getValue("statements");
            if (statements instanceof io.vertx.core.json.JsonArray arr) {
                for (int i = 0; i < arr.size(); i++) {
                    var child = arr.getJsonObject(i);
                    if (child == null) continue;
                    String childIdShort = child.getString("idShort");
                    if (childIdShort == null) continue;
                    String childPath = currentPath + "/" + childIdShort;
                    var found = descendAndMatch(child, childPath, targetPath);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a specific AAS element either from the LIVE backend or from the SNAPSHOT database.
     * @param sourceSystemId ID of the source system.
     * @param smId Submodel ID.
     * @param path Path to the element.
     * @param source Data source (LIVE or SNAPSHOT).
     * @return HTTP Response with the retrieved element.
     */
    @GET
    @Path("/submodels/{smId}/elements/{path:.+}")
    public Response getElement(@PathParam("sourceSystemId") Long sourceSystemId,
                               @PathParam("smId") String smId,
                               @PathParam("path") String path,
                               @QueryParam("source") @DefaultValue("LIVE") String source) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        if ("LIVE".equalsIgnoreCase(source)) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            var resp = traversal.getElement(ss.apiUrl, smId, path, headers).await().indefinitely();
            int sc = resp.statusCode();
            if (sc >= 200 && sc < 300) {
                return Response.ok(resp.bodyAsString()).build();
            }
            var resolved = resolveElementDeep(ss.apiUrl, smId, path, headers);
            if (resolved != null) return Response.ok(resolved.encode()).build();
            return Response.status(sc).entity(resp.bodyAsString()).build();
        }
        
        String normalizedSmId = normalizeSubmodelId(smId);
        var submodel = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", sourceSystemId, normalizedSmId).firstResult();
        if (submodel == null) return Response.status(Response.Status.NOT_FOUND).entity("Submodel not found in snapshot").build();
        var el = AasElementLite.<AasElementLite>find("submodelLite.id = ?1 and idShortPath = ?2", submodel.id, path).firstResult();
        if (el == null) return Response.status(Response.Status.NOT_FOUND).entity("Element not found in snapshot").build();
        io.vertx.core.json.JsonObject json = new io.vertx.core.json.JsonObject()
                .put("idShort", el.idShort)
                .put("idShortPath", el.idShortPath)
                .put("modelType", el.modelType)
                .put("valueType", el.valueType)
                .put("semanticId", el.semanticId);
        return Response.ok(json.encode()).build();
    }

    /**
     * Decodes a Base64-encoded submodel ID if applicable, returning the original string if decoding fails.
     * @param smId The submodel ID string to decode.
     * @return Decoded submodel ID or the original string if decoding fails.
     */
    private String normalizeSubmodelId(String smId) {
        if (smId == null) return null;
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(smId);
            String plain = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            return plain;
        } catch (IllegalArgumentException e) {
            return smId;
        }
    }

}


