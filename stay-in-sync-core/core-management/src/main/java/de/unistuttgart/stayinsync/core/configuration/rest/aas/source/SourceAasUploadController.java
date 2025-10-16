package de.unistuttgart.stayinsync.core.configuration.rest.aas.source;

import de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService;
import io.quarkus.logging.Log;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.http.HttpServerRequest;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.RestForm;

import java.nio.file.Files;

/**
 * REST controller handling AASX (Asset Administration Shell package) uploads for Source Systems.
 * Provides endpoints to upload, preview, and attach AASX content (submodels and elements) to
 * the local AAS snapshot and backend system.
 */
@Path("/api/config/source-system/{sourceSystemId}/aas")
@RegisterForReflection
@Blocking
public class SourceAasUploadController {

    @Inject
    AasStructureSnapshotService snapshotService;

    @Context
    HttpServerRequest request;

    /**
     * Refreshes the AAS snapshot for the specified Source System.
     * Invokes the AasStructureSnapshotService to rebuild the snapshot based on current AAS data.
     *
     * @param sourceSystemId The ID of the Source System.
     * @return HTTP 202 Accepted response once snapshot refresh is triggered.
     */
    @POST
    @Path("/snapshot/refresh")
    public Response refreshSnapshot(@PathParam("sourceSystemId") Long sourceSystemId) {
        snapshotService.refreshSnapshot(sourceSystemId);
        return Response.accepted().build();
    }

    /**
     * Uploads a complete AASX package for ingestion into the snapshot.
     * Attaches all submodels from the AASX file to the existing AAS and refreshes the snapshot.
     *
     * @param sourceSystemId The ID of the Source System.
     * @param file The uploaded AASX file.
     * @return HTTP Response indicating success or failure of the upload.
     */
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
            Log.infof("AASX upload received: sourceSystemId=%d file=%s size=%d bytes", sourceSystemId, filename, fileBytes.length);
            int attached = snapshotService.attachSubmodelsLive(sourceSystemId, fileBytes);
            Log.infof("AASX live attach done: attached=%d. Refreshing snapshot...", attached);
            snapshotService.refreshSnapshot(sourceSystemId);
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

    /**
     * Previews attachable items (submodels or collections) from an uploaded AASX file.
     * Does not persist or modify data; only returns a preview structure.
     *
     * @param sourceSystemId The ID of the Source System.
     * @param file The uploaded AASX file for preview.
     * @return HTTP 200 response containing preview JSON or error message.
     */
    @POST
    @Path("/upload/preview")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview attachable items (submodels and top-level collections/lists) from an AASX file")
    public Response previewAasx(
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
            byte[] fileBytes = Files.readAllBytes(file.uploadedFile());
            var preview = snapshotService.previewAasx(fileBytes);
            return Response.ok(preview.encode()).build();
        } catch (java.io.IOException ioe) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to read uploaded file").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Attaches only selected submodels or collections from an uploaded AASX file.
     * Uses the provided JSON selection to determine which parts to attach, then refreshes the snapshot.
     *
     * @param sourceSystemId The ID of the Source System.
     * @param file The uploaded AASX file.
     * @param selectionJson JSON string specifying which submodels or collections to attach.
     * @return HTTP Response containing details about the attached content.
     */
    @POST
    @Path("/upload/attach-selected")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Attach only selected submodels or top-level collections/lists from an AASX file")
    public Response attachSelected(
            @PathParam("sourceSystemId") Long sourceSystemId,
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
            int attached = snapshotService.attachSelectedFromAasx(sourceSystemId, fileBytes, selection);
            Log.infof("Attach-selected done: attached=%d → refreshing snapshot", attached);
            snapshotService.refreshSnapshot(sourceSystemId);
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
