package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationScriptMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationAssemblyDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationDetailsDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationStatusUpdate;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.UpdateTransformationRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TargetDtsBuilderGeneratorService;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationService;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/transformation")
@Produces(APPLICATION_JSON)
@Tag(name = "Transformation Configuration", description = "Endpoints for managing transformations")
public class TransformationResource {

    @Inject
    TransformationService transformationService;

    @Inject
    TargetDtsBuilderGeneratorService targetDtsGeneratorService;

    @Inject
    TransformationMapper mapper;

    @Inject
    TransformationScriptMapper scriptMapper;

    @Inject
    @Channel("transformation-status-updates")
    Multi<TransformationStatusUpdate> statusUpdates;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a new transformation shell",
            description = "Creates the initial transformation object with basic info like name and description. Returns the created object with its new ID.")
    public Response createTransformationShell(TransformationShellDTO dto, @Context UriInfo uriInfo) {
        var persisted = transformationService.createTransformation(dto);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persisted.id));
        Log.debugf("New transformation shell created with URI %s", builder.build().toString());

        return Response.created(builder.build()).entity(mapper.mapToDetailsDTO(persisted)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Assembles a transformation with its related components",
            description = "Updates an existing transformation shell by linking it to a script, rule, and endpoints using their IDs.")
    public Response assembleTransformation(@Parameter(name = "id", required = true) @PathParam("id") Long id, TransformationAssemblyDTO dto) {
        if (!id.equals(dto.id())) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "ID Mismatch",
                    "The ID in the path (%d) does not match the ID in the request body (%d).", id, dto.id());
        }

        var updated = transformationService.updateTransformation(id, dto);
        Log.debugf("Transformation with id %d was assembled.", id);
        return Response.ok(mapper.mapToDetailsDTO(updated)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a transformation for a given identifier")
    public Response getTransformation(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return transformationService.findById(id)
                .map(transformation -> {
                    Log.debugf("Found transformation: %s", transformation);
                    return Response.ok(mapper.mapToDetailsDTO(transformation)).build();
                })
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find transformation", "No transformation found using id %d", id));
    }

    @GET
    @Operation(summary = "Returns all transformations")
    public List<TransformationDetailsDTO> getAllTransformations(@Parameter(name = "with_syncjob_filter", description = "An optional filter parameter to filter transformations") @QueryParam("withSyncJob") Optional<Boolean> withSyncJob,
                                                                   @Parameter(name = "syncJobId", description = "An optional filter parameter to filter transformations") @QueryParam("syncJobId") Optional<Long> syncJobId) {
        List<Transformation> transformations;
        if (withSyncJob.isPresent() && syncJobId.isPresent()) {
            throw new CoreManagementException(Response.Status.BAD_REQUEST, "Invalid query params", "Cannot use syncJobId and withSyncJob params together");
        }
        if (withSyncJob.isPresent()) {
            transformations = withSyncJob.map(this.transformationService::findAllWithSyncJobFilter).orElseGet(transformationService::findAll);
        } else if (syncJobId.isPresent()) {
            transformations = transformationService.findAllBySyncjob(syncJobId.get());
        } else {
            transformations = transformationService.findAll();
        }


        Log.debugf("Total number of transformations: %d", transformations.size());
        return transformations.stream()
                .map(mapper::mapToDetailsDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}/transformation-script")
    @Operation(summary = "Returns the associated transformation script for this Transformation.")
    public Response getTransformationScript(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return transformationService.findScriptById(id)
                .map(script -> {
                    Log.debugf("Found transformation script: %s", script);
                    return Response.ok(scriptMapper.mapToDTO(script)).build();
                })
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find transformation script", "No script is assigned to Transformation with id %d", id));
    }

    @GET
    @Path("/{id}/target-arcs")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Gets the associated Target ARCs for a Transformation",
            description = "Provides the currently actively bound Target ARCs for a Transformation")
    public Response getTransformationTargetArcs(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return Response.ok(transformationService.getTargetArcs(id)).build();
    }

    @PUT
    @Path("/{id}/target-arcs")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Updates the associated Target ARCs for a Transformation",
            description = "Replaces the entire set of linked Target ARCs for a transformation with the given list of ARC IDs.")
    public Response updateTransformationTargetArcs(@PathParam("id") Long id, @Valid UpdateTransformationRequestConfigurationDTO dto) {
        var updated = transformationService.updateTargetArcs(id, dto);
        return Response.ok(updated).build();
    }

    @GET
    @Path("/{id}/target-type-definitions")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Returns all necessary TypeScript builder type definitions for the Monaco editor",
            description = "Provides a structured set of .d.ts libraries for all Target ARCs linked to this transformation.")
    public Response getTargetTypeDefinitions(@PathParam("id") Long id) {
        return Response.ok(targetDtsGeneratorService.generateForTransformation(id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes an existing transformation")
    public Response deleteTransformation(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        boolean deleted = transformationService.delete(id);

        if (deleted) {
            Log.debugf("Transformation with id %d deleted", id);
            return Response.noContent().build();
        } else {
            Log.warnf("Attempted to delete non-existent transformation script with id %d", id);
            throw new CoreManagementException(Response.Status.NOT_FOUND,
                    "Transformation not found",
                    "Transformation with id %d could not be found for deletion.", id);
        }
    }

    @PUT
    @Path("/{id}/deployment")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Enables deploying or undeploying of transformations",
            description = "Allows managing the deployment status of a transformation transformation")
    public Response manageDeployment(@Parameter(name = "id", required = true) @PathParam("id") Long id, JobDeploymentStatus deploymentStatus) {
        transformationService.updateDeploymentStatus(id, deploymentStatus);
        return Response.noContent().build();
    }


    @PUT
    @Path("/{id}/rule/{ruleId}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Enables managing the rule applied to the transformation execution",
            description = "Updates an existing transformation")
    public Response addRule(@Parameter(name = "id", required = true) @PathParam("id") Long id, @Parameter(name = "ruleId", required = true) @PathParam("ruleId") Long ruleId) {
        transformationService.addRule(id, ruleId);
        Log.debugf("Added rule with id %d, to transformation with id: %d", ruleId, id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/rule")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Enables managing the rule applied to the transformation execution",
            description = "Updates an existing transformation")
    public Response removeRule(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        transformationService.removeRule(id);
        Log.debugf("Removed rule from transformation with id: %d", id);
        return Response.noContent().build();
    }


    @GET
    @Path("/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<TransformationStatusUpdate> streamDeploymentStatus(@Parameter(name = "syncJobId", description = "Filters status updates by syncjob") @QueryParam("syncJobId") Long syncJobId) {
        return statusUpdates
                .filter(update -> {
                    boolean matches = update.syncJobId().equals(syncJobId);
                    Log.infof("Filter check - syncJobId: %s, update.syncJobId: %s, matches: %s",
                            syncJobId, update.syncJobId(), matches);
                    return matches;
                })
                .onItem().invoke(update -> Log.infof("Filtered update being sent to client: %s", update))
                .onCompletion().invoke(() -> Log.info("Transformation deployment status stream completed"))
                .onCancellation().invoke(() -> Log.info("Transformation deployment status  stream cancelled by client"));
    }
}
