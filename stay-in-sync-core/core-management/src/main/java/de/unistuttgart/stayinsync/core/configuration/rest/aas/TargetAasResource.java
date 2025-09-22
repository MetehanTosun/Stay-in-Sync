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
    public Uni<Response> listSubmodels(@PathParam("targetSystemId") Long targetSystemId) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        return traversal.listSubmodels(ts.apiUrl, ts.aasId, headers)
                .map(resp -> Response.status(resp.statusCode()).entity(resp.bodyAsString()).build());
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
        return traversal.listElements(ts.apiUrl, smId, depth, parentPath, headers)
                .map(resp -> Response.status(resp.statusCode()).entity(resp.bodyAsString()).build());
    }

    @GET
    @Path("/submodels/{smId}/elements/{path:.+}")
    public Response getElement(@PathParam("targetSystemId") Long targetSystemId,
                               @PathParam("smId") String smId,
                               @PathParam("path") String path) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        var resp = traversal.getElement(ts.apiUrl, smId, path, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
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
                var parentResp = traversal.getElement(ts.apiUrl, smId, parentPath, headers).await().indefinitely();
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
            Log.infof("AASX upload received (target): targetSystemId=%d file=%s size=%d bytes", targetSystemId, filename, fileBytes.length);
            int attached = snapshotService.attachSubmodelsLiveToTarget(targetSystemId, fileBytes);
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
            // Attach upstream to target AAS (same logic as source), then return count
            int attached = snapshotService.attachSelectedFromAasxToTarget(targetSystemId, fileBytes, selection);
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



