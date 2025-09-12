package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SyncJobFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobCreationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SyncJobService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;


import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/sync-job")
@Produces(APPLICATION_JSON)
public class SyncJobResource {
    @Inject
    SyncJobService syncJobService;

    @Inject
    SyncJobFullUpdateMapper fullUpdateMapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a valid sync-job")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created sync-job",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid sync-job passed in (or no request body found)"
    )
    public Response createSyncJob(
            @RequestBody(
                    name = "sync-job",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = SyncJobCreationDTO.class),
                            examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_SYNCJOB_TO_CREATE)
                    )
            )
            @Valid @NotNull SyncJobCreationDTO syncJobDTO,
            @Context UriInfo uriInfo) {
        var persistedSyncJob = this.syncJobService.persistSyncJob(syncJobDTO);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedSyncJob.id));
        Log.debugf("New sync-job created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }

    @GET
    @Operation(summary = "Returns all the sync-jobs from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all sync-jobs",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = SyncJobDTO.class, type = SchemaType.ARRAY)
            )
    )
    public List<SyncJobDTO> getAllSyncJobs(@Parameter(name = "name_filter", description = "An optional filter parameter to filter results by name") @QueryParam("name_filter") Optional<String> nameFilter) {
        var syncJobs = nameFilter
                .map(this.syncJobService::findAllSyncJobsHavingName)
                .orElseGet(this.syncJobService::findAllSyncJobs);

        Log.debugf("Total number of sync-jobs: %d", syncJobs.size());

        return fullUpdateMapper.mapToDTOList(syncJobs);
    }


    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a sync-job for a given identifier")
    @APIResponse(
            responseCode = "200",
            description = "Gets a sync-job for a given id",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = SyncJobDTO.class),
                    examples = @ExampleObject(name = "sync-job", value = Examples.VALID_EXAMPLE_SYNCJOB)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The sync-job is not found for a given identifier"
    )
    public Response getSyncJob(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.syncJobService.findSyncJobById(id)
                .map(syncJob -> {
                    Log.debugf("Found sync-job: %s", syncJob);
                    return Response.ok(fullUpdateMapper.mapToDTO(syncJob)).build();
                })
                .orElseThrow(() -> {
                    Log.warnf("No sync-job found using id %d", id);
                    return new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find sync-job", "No sync-job found using id %d", id);
                });
    }

    @DELETE
    @Operation(summary = "Deletes an exiting sync-job")
    @APIResponse(
            responseCode = "204",
            description = "Delete a sync-job"
    )
    @Path("/{id}")
    public void deleteSyncJob(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.syncJobService.deleteSyncJob(id);
        Log.debugf("Sync-job with id %d deleted ", id);
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates an exiting sync-job by replacing it with the passed-in sync-job")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the sync-job"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid sync-job passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No sync-job found"
    )
    public Response fullyUpdateSyncJob(@Parameter(name = "id", required = true)
                                       @RequestBody(
                                               name = "sync-job",
                                               required = true,
                                               content = @Content(
                                                       mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = SyncJobDTO.class),
                                                       examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_SYNCJOB)
                                               )
                                       )
                                       @PathParam("id") Long id, @Valid @NotNull SyncJobCreationDTO syncJobDTO) {
        if (!Objects.equals(id, syncJobDTO.id())) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Id missmatch", "Make sure that the request body entity id matches the request parameter");
        }

        var updatedSyncJob = this.syncJobService.replaceSyncJob(syncJobDTO);
        if (updatedSyncJob != null) {
            Log.debugf("Sync-job replaced with new values %s", updatedSyncJob);
            return Response.noContent().build();
        } else {
            Log.debugf("No sync-job found with id %d", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
