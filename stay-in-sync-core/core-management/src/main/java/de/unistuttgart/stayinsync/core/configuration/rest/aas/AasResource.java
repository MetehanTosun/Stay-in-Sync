package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasTestResultDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelElementNodeDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelSummaryDTO;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasElementLite;
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


@Path("/api/config/source-system/{sourceSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class AasResource {


    @Inject
    SourceSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    AasStructureSnapshotService snapshotService;

    @Inject
    de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder headerBuilder;

    @POST
    @Path("/test")
    public Uni<Response> test(@PathParam("sourceSystemId") Long sourceSystemId) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        Log.infof("AAS test: sourceSystemId=%d apiUrl=%s aasId=%s", sourceSystemId, ss.apiUrl, ss.aasId);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
        Log.debugf("AAS test headers: %s", headers);
        Uni<HttpResponse<Buffer>> uni = traversal.getShell(ss.apiUrl, ss.aasId, headers);
        return uni.map(resp -> {
                    int sc = resp.statusCode();
                    Log.infof("AAS test upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
                    if (sc >= 200 && sc < 300) {
                        return Response.ok(new AasTestResultDTO("shell", "asset")).build();
                    }
                    Log.warnf("AAS test failed: %d %s", sc, resp.statusMessage());
                    return Response.status(sc).entity(resp.bodyAsString()).build();
                });
    }

    @GET
    @Path("/submodels")
    public Uni<Response> listSubmodels(@PathParam("sourceSystemId") Long sourceSystemId,
                                  @QueryParam("source") @DefaultValue("SNAPSHOT") String source) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        if ("LIVE".equalsIgnoreCase(source)) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            Log.infof("List submodels LIVE: apiUrl=%s aasId=%s", ss.apiUrl, ss.aasId);
            Log.debugf("LIVE headers: %s", headers);
            return traversal.listSubmodels(ss.apiUrl, ss.aasId, headers).map(resp -> {
                int sc = resp.statusCode();
                Log.infof("List submodels upstream status=%d msg=%s", sc, resp.statusMessage());
                if (sc >= 200 && sc < 300) {
                    return Response.ok(resp.bodyAsString()).build();
                }
                return Response.status(sc).entity(resp.bodyAsString()).build();
            });
        }
        var list = AasSubmodelLite.<AasSubmodelLite>list("sourceSystem.id", sourceSystemId)
                .stream()
                .map(sm -> new SubmodelSummaryDTO(sm.submodelId, sm.submodelIdShort, sm.semanticId, sm.kind, sm.id))
                .toList();
        if (!list.isEmpty()) {
            return Uni.createFrom().item(Response.ok().entity(list).build());
        }
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
        return traversal.listSubmodels(ss.apiUrl, ss.aasId, headers).map(resp -> {
            int sc = resp.statusCode();
            if (sc >= 200 && sc < 300) {
                return Response.ok(resp.bodyAsString()).build();
            }
            return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        });
    }

    @GET
    @Path("/submodels/{smId}/elements")
    public Uni<Response> listElements(@PathParam("sourceSystemId") Long sourceSystemId,
                                 @PathParam("smId") String smId,
                                 @QueryParam("depth") @DefaultValue("shallow") String depth,
                                 @QueryParam("parentPath") String parentPath,
                                 @QueryParam("source") @DefaultValue("SNAPSHOT") String source) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        if ("LIVE".equalsIgnoreCase(source)) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            return traversal.listElements(ss.apiUrl, smId, depth, parentPath, headers).map(resp -> {
                int sc = resp.statusCode();
                if (sc >= 200 && sc < 300) {
                    return Response.ok(resp.bodyAsString()).build();
                }
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
                    return Response.ok(resp.bodyAsString()).build();
                }
                return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
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
                return Response.ok(resp.bodyAsString()).build();
            }
            return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        });
    }

    @POST
    @Path("/submodels")
    public Response createSubmodel(@PathParam("sourceSystemId") Long sourceSystemId, String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("Create submodel LIVE: apiUrl=%s", ss.apiUrl);
        Log.debugf("WRITE headers: %s body=%s", headers, body);
        var resp = traversal.createSubmodel(ss.apiUrl, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("Create submodel upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            try {
                String submodelId = io.vertx.core.json.JsonObject.mapFrom(io.vertx.core.json.jackson.DatabindCodec.mapper().readTree(resp.bodyAsString())).getString("id");
                if (submodelId == null || submodelId.isBlank()) {
                    var reqId = io.vertx.core.json.JsonObject.mapFrom(io.vertx.core.json.jackson.DatabindCodec.mapper().readTree(body)).getString("id");
                    if (reqId != null && !reqId.isBlank()) submodelId = reqId;
                }
                if (submodelId != null && !submodelId.isBlank()) {
                    var refHeaders = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
                    var refResp = traversal.addSubmodelReferenceToShell(ss.apiUrl, ss.aasId, submodelId, refHeaders).await().indefinitely();
                    Log.infof("Add submodel-ref upstream status=%d msg=%s body=%s", refResp.statusCode(), refResp.statusMessage(), safeBody(refResp));
                } else {
                    Log.warn("Could not resolve submodelId for auto-referencing");
                }
            } catch (Exception e) {
                Log.warn("Auto-reference add failed", e);
            }
            snapshotService.applySubmodelCreate(sourceSystemId, resp.bodyAsString());
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

    @PUT
    @Path("/submodels/{smId}")
    public Response putSubmodel(@PathParam("sourceSystemId") Long sourceSystemId,
                                @PathParam("smId") String smId,
                                String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("PUT submodel LIVE: apiUrl=%s smId=%s", ss.apiUrl, smId);
        var resp = traversal.putSubmodel(ss.apiUrl, smId, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("PUT submodel upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            snapshotService.applySubmodelCreate(sourceSystemId, resp.bodyAsString());
            return Response.ok(resp.bodyAsString()).build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

    @DELETE
    @Path("/submodels/{smId}")
    public Response deleteSubmodel(@PathParam("sourceSystemId") Long sourceSystemId,
                                   @PathParam("smId") String smId) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("DELETE submodel LIVE: apiUrl=%s smId=%s", ss.apiUrl, smId);
        // Remove reference from shell first (best-effort)
        try {
            var refDel = traversal.removeSubmodelReferenceFromShell(ss.apiUrl, ss.aasId, smId, headers).await().indefinitely();
            Log.infof("DELETE submodel-ref upstream status=%d msg=%s body=%s", refDel.statusCode(), refDel.statusMessage(), safeBody(refDel));
        } catch (Exception e) {
            Log.warn("Failed to remove submodel-ref from shell (continuing)", e);
        }
        var resp = traversal.deleteSubmodel(ss.apiUrl, smId, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("DELETE submodel upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            snapshotService.applySubmodelDelete(sourceSystemId, smId);
            return Response.noContent().build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

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
        var resp = traversal.createElement(ss.apiUrl, smId, parentPath, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("Create element upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            snapshotService.applyElementCreate(sourceSystemId, smId, parentPath, resp.bodyAsString());
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

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
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

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
            snapshotService.applyElementDelete(sourceSystemId, smId, path);
            return Response.noContent().build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }
    @PATCH
    @Path("/submodels/{smId}/elements/{path:.+}/value")
    public Response patchElementValue(@PathParam("sourceSystemId") Long sourceSystemId,
                                      @PathParam("smId") String smId,
                                      @PathParam("path") String path,
                                      String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("Patch element value LIVE: apiUrl=%s smId=%s path=%s", ss.apiUrl, smId, path);
        Log.debugf("WRITE headers: %s body=%s", headers, body);
        var resp = traversal.patchElementValue(ss.apiUrl, smId, path, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("Patch element upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            return Response.noContent().build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

    private String safeBody(HttpResponse<Buffer> resp) {
        try {
            String b = resp.bodyAsString();
            if (b == null) return null;
            return b.length() > 500 ? b.substring(0, 500) + "..." : b;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeSubmodelId(String smId) {
        if (smId == null) return null;
        try {
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(smId);
            String plain = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            return plain;
        } catch (IllegalArgumentException e) {
            return smId;
        }
    }

    @POST
    @Path("/snapshot/refresh")
    public Response refreshSnapshot(@PathParam("sourceSystemId") Long sourceSystemId) {
        snapshotService.refreshSnapshot(sourceSystemId);
        return Response.accepted().build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload a standard AASX file and ingest its submodels/elements into the snapshot")
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Upload accepted, snapshot ingestion started"),
            @APIResponse(responseCode = "400", description = "Invalid AASX"),
            @APIResponse(responseCode = "409", description = "Duplicate IDs detected for this source system")
    })
    public Response uploadAasx(
            @PathParam("sourceSystemId") Long sourceSystemId,
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
            snapshotService.ingestAasx(sourceSystemId, filename, fileBytes);
            return Response.accepted().build();
        } catch (java.io.IOException ioe) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to read uploaded file").build();
        } catch (de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService.DuplicateIdException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        } catch (de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService.InvalidAasxException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}


