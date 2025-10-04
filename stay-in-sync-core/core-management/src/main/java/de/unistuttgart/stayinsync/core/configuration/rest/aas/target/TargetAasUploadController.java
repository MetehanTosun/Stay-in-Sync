package de.unistuttgart.stayinsync.core.configuration.rest.aas.target;

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

@Path("/api/config/target-system/{targetSystemId}/aas")
@RegisterForReflection
@Blocking
public class TargetAasUploadController {

    @Inject
    AasStructureSnapshotService snapshotService;

    @Context
    HttpServerRequest request;

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
