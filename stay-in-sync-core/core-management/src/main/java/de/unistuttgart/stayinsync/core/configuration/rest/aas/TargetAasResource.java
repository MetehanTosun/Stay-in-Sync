package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.TargetSystemAasService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder;
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

@Path("/api/config/target-system/{targetSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class TargetAasResource {

    @Inject
    TargetSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    AasStructureSnapshotService snapshotService;

    @Inject
    HttpHeaderBuilder headerBuilder;

    @POST
    @Path("/test")
    public Uni<Response> test(@PathParam("targetSystemId") Long targetSystemId) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        Log.infof("AAS test (target): id=%d apiUrl=%s aasId=%s", targetSystemId, ts.apiUrl, ts.aasId);
        Uni<HttpResponse<Buffer>> uni = traversal.getShell(ts.apiUrl, ts.aasId, headers);
        return uni.map(resp -> Response.status(resp.statusCode()).entity(resp.bodyAsString()).build());
    }

    @GET
    @Path("/submodels")
    public Response listSubmodels(@PathParam("targetSystemId") Long targetSystemId) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        try {
            var refsResp = traversal.listSubmodels(ts.apiUrl, ts.aasId, headers).await().indefinitely();
            if (refsResp.statusCode() < 200 || refsResp.statusCode() >= 300) {
                return Response.status(refsResp.statusCode()).entity(refsResp.bodyAsString()).build();
            }
            String body = refsResp.bodyAsString();
            Log.infof("Target listSubmodels: upstream status=%d, bodyLen=%d", refsResp.statusCode(), (body == null ? 0 : body.length()));
            io.vertx.core.json.JsonArray refs;
            boolean wrapped = false;
            try {
                if (body != null && body.trim().startsWith("{")) {
                    io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(body);
                    refs = obj.getJsonArray("result", new io.vertx.core.json.JsonArray());
                    wrapped = true;
                } else {
                    refs = body != null ? new io.vertx.core.json.JsonArray(body) : new io.vertx.core.json.JsonArray();
                }
            } catch (Exception e) {
                refs = new io.vertx.core.json.JsonArray();
            }
            Log.infof("Target listSubmodels: parsed refs count=%d (wrapped=%s)", refs.size(), wrapped);
            io.vertx.core.json.JsonArray out = new io.vertx.core.json.JsonArray();
            for (int i = 0; i < refs.size(); i++) {
                var ref = refs.getJsonObject(i);
                String submodelId = null;
                try {
                    io.vertx.core.json.JsonArray keys = ref.getJsonArray("keys");
                    if ((keys == null || keys.isEmpty()) && ref.containsKey("value")) {
                        var val = ref.getJsonObject("value");
                        if (val != null) keys = val.getJsonArray("keys");
                    }
                    if (keys != null && !keys.isEmpty()) {
                        var k0 = keys.getJsonObject(0);
                        if (k0 != null && "Submodel".equalsIgnoreCase(k0.getString("type"))) {
                            submodelId = k0.getString("value");
                        } else {
                            submodelId = k0 != null ? k0.getString("value") : null;
                        }
                    }
                } catch (Exception ignore) { }
                Log.infof("Target listSubmodels: ref[%d] -> submodelId=%s", i, submodelId);
                if (submodelId == null) continue;
                try {
                    String normalizedSmId = normalizeSubmodelId(submodelId);
                    String smIdB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(normalizedSmId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    var smResp = traversal.getSubmodel(ts.apiUrl, smIdB64, headers).await().indefinitely();
                    if (smResp != null && smResp.statusCode() >= 200 && smResp.statusCode() < 300) {
                        String smBody = smResp.bodyAsString();
                        var sm = smBody != null && smBody.trim().startsWith("{") ? new io.vertx.core.json.JsonObject(smBody) : new io.vertx.core.json.JsonObject();
                        String id = sm.getString("id", normalizedSmId);
                        String idShort = sm.getString("idShort");
                        if (idShort == null || idShort.isBlank()) {
                            idShort = deriveIdShortFromId(id);
                        }
                        Log.infof("Target listSubmodels: fetched submodel id=%s idShort=%s kind=%s", id, idShort, sm.getString("kind"));
                        io.vertx.core.json.JsonObject item = new io.vertx.core.json.JsonObject()
                                .put("id", id)
                                .put("submodelId", id)
                                .put("idShort", idShort)
                                .put("submodelIdShort", idShort)
                                .put("kind", sm.getString("kind"));
                        out.add(item);
                    } else {
                        Log.infof("Target listSubmodels: filtered stale/invalid submodel-ref value=%s status=%d", normalizedSmId, (smResp != null ? smResp.statusCode() : -1));
                    }
                } catch (Exception ex) {
                    Log.infof("Target listSubmodels: exception resolving submodel %s, skipping: %s", submodelId, ex.getMessage());
                }
            }
            Log.infof("Target listSubmodels: final items=%d", out.size());
            if (wrapped) {
                io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject().put("result", out);
                return Response.ok(obj.encode()).build();
            }
            return Response.ok(out.encode()).build();
        } catch (Exception e) {
            // Fallback: pass-through refs on error
            var fallback = traversal.listSubmodels(ts.apiUrl, ts.aasId, headers).await().indefinitely();
            return Response.status(fallback.statusCode()).entity(fallback.bodyAsString()).build();
        }
    }

    private String normalizeSubmodelId(String smId) {
        if (smId == null) return null;
        try {
            String s = smId.replace('-', '+').replace('_', '/');
            int pad = (4 - (s.length() % 4)) % 4;
            if (pad > 0) {
                s = s + "====".substring(0, pad);
            }
            byte[] decoded = java.util.Base64.getDecoder().decode(s);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return smId;
        }
    }

    private String deriveIdShortFromId(String id) {
        if (id == null) return null;
        String s = id;
        int hash = s.lastIndexOf('#');
        int slash = s.lastIndexOf('/');
        // Split by '/'
        String[] parts = s.split("/");
        if (parts.length >= 3) {
            String last = parts[parts.length - 1];
            String prev = parts[parts.length - 2];
            String prev2 = parts[parts.length - 3];
            boolean lastNum = last.matches("\\d+");
            boolean prevNum = prev.matches("\\d+");
            if (lastNum && prevNum) {
                // pattern .../<name>/<version>/<revision>
                return prev2;
            }
            if (lastNum && !prevNum) {
                // pattern .../<name>/<number>
                return prev;
            }
        }
        int idx = Math.max(hash, slash);
        if (idx >= 0 && idx < s.length() - 1) return s.substring(idx + 1);
        return s;
    }

    @GET
    @Path("/submodels/{smId}/elements")
	public Uni<Response> listElements(@PathParam("targetSystemId") Long targetSystemId,
									 @PathParam("smId") String smId,
									 @QueryParam("depth") @DefaultValue("shallow") String depth,
									 @QueryParam("parentPath") String parentPath) {
		TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
		ts = aasService.validateAasTarget(ts);
		var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
		final String apiUrl = ts.apiUrl;
		final java.util.Map<String,String> headersLive = headers;
		return traversal.listElements(apiUrl, smId, depth, parentPath, headersLive).map(resp -> {
			int sc = resp.statusCode();
			if (sc >= 200 && sc < 300) {
				return Response.ok(resp.bodyAsString()).build();
			}
			// Fallback for nested collections returning 404 on parent path: fetch deep and filter
			if (sc == 404 && parentPath != null && !parentPath.isBlank()) {
				try {
					var all = traversal.listElements(apiUrl, smId, "all", null, headersLive).await().indefinitely();
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
							if (p.equals(parentPath)) continue;
							if (p.startsWith(prefix)) {
								String rest = p.substring(prefix.length());
								if (!rest.contains("/")) {
									children.add(el);
								}
							}
						}
						return Response.ok(children.encode()).build();
					}
				} catch (Exception ignore) {}
			}
			return Response.status(sc).entity(resp.bodyAsString()).build();
		});
	}

    @GET
    @Path("/submodels/{smId}/elements/{path:.+}")
    public Response getElement(@PathParam("targetSystemId") Long targetSystemId,
                               @PathParam("smId") String smId,
                               @PathParam("path") String path) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        String normalizedSmId = normalizeSubmodelId(smId);
        Log.infof("Target getElement: apiUrl=%s smId(raw)=%s smId(norm)=%s path=%s", ts.apiUrl, smId, normalizedSmId, path);
        // Use raw Base64URL for upstream
        var resp = traversal.getElement(ts.apiUrl, smId, path, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("Target getElement upstream status=%d msg=%s", sc, resp.statusMessage());
        if (sc >= 200 && sc < 300) {
            return Response.ok(resp.bodyAsString()).build();
        }
        // Fallback like Source: deep resolve
        try {
            var all = traversal.listElements(ts.apiUrl, smId, "all", null, headers).await().indefinitely();
            if (all.statusCode() >= 200 && all.statusCode() < 300) {
                String body = all.bodyAsString();
                io.vertx.core.json.JsonArray root = body != null && body.trim().startsWith("{")
                        ? new io.vertx.core.json.JsonObject(body).getJsonArray("result", new io.vertx.core.json.JsonArray())
                        : new io.vertx.core.json.JsonArray(body);
                io.vertx.core.json.JsonObject found = findElementByPathRecursive(root, path);
                if (found != null) return Response.ok(found.encode()).build();
            }
        } catch (Exception ignore) {}
        return Response.status(sc).entity(resp.bodyAsString()).build();
    }

    private io.vertx.core.json.JsonObject findElementByPathRecursive(io.vertx.core.json.JsonArray elements, String targetPath) {
        if (elements == null) return null;
        for (int i = 0; i < elements.size(); i++) {
            var el = elements.getJsonObject(i);
            if (el == null) continue;
            String idShort = el.getString("idShort");
            if (idShort == null || idShort.isBlank()) continue;
            var found = descendAndMatch(el, idShort, targetPath);
            if (found != null) return found;
        }
        return null;
    }

    private io.vertx.core.json.JsonObject descendAndMatch(io.vertx.core.json.JsonObject element, String currentPath, String targetPath) {
        if (targetPath.equals(currentPath)) return element;
        String modelType = element.getString("modelType");
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

    @POST
    @Path("/submodels")
    public Response createSubmodel(@PathParam("targetSystemId") Long targetSystemId, String body) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.createSubmodel(ts.apiUrl, body, headers).await().indefinitely();
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        return aasService.mapHttpError(resp.statusCode(), resp.statusMessage(), resp.bodyAsString());
    }

    @PUT
    @Path("/submodels/{smId}")
    public Response putSubmodel(@PathParam("targetSystemId") Long targetSystemId,
                                @PathParam("smId") String smId,
                                String body) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.putSubmodel(ts.apiUrl, smId, body, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }

    @DELETE
    @Path("/submodels/{smId}")
    public Response deleteSubmodel(@PathParam("targetSystemId") Long targetSystemId,
                                   @PathParam("smId") String smId) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.deleteSubmodel(ts.apiUrl, smId, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }

    @POST
    @Path("/submodels/{smId}/elements")
    public Response createElement(@PathParam("targetSystemId") Long targetSystemId,
                                  @PathParam("smId") String smId,
                                  @QueryParam("parentPath") String parentPath,
                                  String body) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        // Resolve parent path for collections/lists/entities like in Source AAS resource
        String effectiveParentPath = parentPath;
        if (parentPath != null && !parentPath.isBlank()) {
            try {
                String normalizedSmId = normalizeSubmodelId(smId);
                var parentResp = traversal.getElement(ts.apiUrl, normalizedSmId, parentPath, headers).await().indefinitely();
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
            } catch (Exception ignore) { }
        }
        // Use raw smId (Base64URL) for upstream
        var resp = traversal.createElement(ts.apiUrl, smId, effectiveParentPath, body, headers).await().indefinitely();
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        return aasService.mapHttpError(resp.statusCode(), resp.statusMessage(), resp.bodyAsString());
    }

    @PUT
    @Path("/submodels/{smId}/elements/{path:.+}")
    public Response putElement(@PathParam("targetSystemId") Long targetSystemId,
                               @PathParam("smId") String smId,
                               @PathParam("path") String path,
                               String body) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.putElement(ts.apiUrl, smId, path, body, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }

    @DELETE
    @Path("/submodels/{smId}/elements/{path:.+}")
    public Response deleteElement(@PathParam("targetSystemId") Long targetSystemId,
                                  @PathParam("smId") String smId,
                                  @PathParam("path") String path) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.deleteElement(ts.apiUrl, smId, path, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }

    @PATCH
    @Path("/submodels/{smId}/elements/{path:.+}/value")
    public Response patchElementValue(@PathParam("targetSystemId") Long targetSystemId,
                                      @PathParam("smId") String smId,
                                      @PathParam("path") String path,
                                      String body) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.patchElementValue(ts.apiUrl, smId, path, body, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }

    @POST
    @Path("/upload/preview")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview attachable items (submodels and top-level collections/lists) from an AASX file (target)")
    public Response previewAasx(
            @PathParam("targetSystemId") Long targetSystemId,
            @RequestBody(required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = Object.class)))
            @RestForm("file") FileUpload file
    ) {
        try {
            if (file == null || file.size() == 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Missing file").build();
            }
            byte[] fileBytes = Files.readAllBytes(file.uploadedFile());
            var preview = snapshotService.previewAasx(fileBytes);
            return Response.ok(preview.encode()).build();
        } catch (java.io.IOException ioe) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to read uploaded file").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload a standard AASX file and attach its submodels to the target AAS")
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Upload accepted"),
            @APIResponse(responseCode = "400", description = "Invalid AASX"),
            @APIResponse(responseCode = "409", description = "Duplicate IDs detected for this target system")
    })
    public Response uploadAasx(
            @PathParam("targetSystemId") Long targetSystemId,
            @RequestBody(required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = Object.class)))
            @RestForm("file") FileUpload file
    ) {
		try {
            if (file == null || file.size() == 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Missing file").build();
            }
            String filename = file.fileName();
            byte[] fileBytes = Files.readAllBytes(file.uploadedFile());
			Log.infof("TargetAasResource.uploadAasx: received targetSystemId=%d file=%s size=%d bytes", targetSystemId, filename, fileBytes.length);
            int attached = snapshotService.attachSubmodelsLiveToTarget(targetSystemId, fileBytes);
			Log.infof("TargetAasResource.uploadAasx: attachSubmodelsLiveToTarget attached=%d", attached);
            // Mirror Source behavior: trigger a lightweight refresh on Source side; for Target we simply return accepted
            io.vertx.core.json.JsonObject result = new io.vertx.core.json.JsonObject()
                    .put("filename", filename)
                    .put("attachedSubmodels", attached);
            return Response.accepted(result.encode()).build();
        } catch (java.io.IOException ioe) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to read uploaded file").build();
        } catch (de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService.DuplicateIdException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        } catch (de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService.InvalidAasxException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/upload/attach-selected")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Attach only selected submodels or top-level collections/lists from an AASX file (target)")
    public Response attachSelected(
            @PathParam("targetSystemId") Long targetSystemId,
            @RequestBody(required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = Object.class)))
            @RestForm("file") FileUpload file,
            @RestForm("selection") String selectionJson
    ) {
		try {
            if (file == null || file.size() == 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Missing file").build();
            }
            byte[] fileBytes = Files.readAllBytes(file.uploadedFile());
            io.vertx.core.json.JsonObject selection = null;
            if (selectionJson != null && !selectionJson.isBlank()) {
                selection = new io.vertx.core.json.JsonObject(selectionJson);
            }
			Log.infof("TargetAasResource.attachSelected: targetSystemId=%d selection=%s", targetSystemId, selection);
            int attached = snapshotService.attachSelectedFromAasxToTarget(targetSystemId, fileBytes, selection);
			Log.infof("TargetAasResource.attachSelected: attached=%d", attached);
            io.vertx.core.json.JsonObject result = new io.vertx.core.json.JsonObject()
                    .put("attachedSubmodels", attached)
                    .put("selection", selection);
            return Response.accepted(result.encode()).build();
        } catch (java.io.IOException ioe) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to read uploaded file").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}



