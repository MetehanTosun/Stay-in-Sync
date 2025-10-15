package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TargetSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateTargetSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TargetSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TypeScriptGenerationRequest;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TypeScriptGenerationResponse;
import de.unistuttgart.stayinsync.core.configuration.service.TargetSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.util.TypeScriptTypeGenerator;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST resource for managing Target System Endpoints.
 * Provides CRUD operations for endpoints associated with a Target System
 * and supports TypeScript interface generation from JSON schemas.
 */
@Path("api/config/target-systems/")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class TargetSystemEndpointResource {

    @Inject
    TargetSystemEndpointService service;

    @Inject
    TargetSystemEndpointFullUpdateMapper mapper;

    @Inject
    TypeScriptTypeGenerator typeScriptTypeGenerator;

    /**
     * Creates one or multiple new Target System Endpoints for a given Target System.
     *
     * @param targetSystemId The ID of the Target System for which endpoints are created.
     * @param dtos List of CreateTargetSystemEndpointDTO containing endpoint creation data.
     * @param uriInfo Context information for URI building.
     * @return HTTP 201 response containing the created Target System Endpoints.
     */
    @POST
    @Path("{targetSystemId}/endpoints")
    @Operation(summary = "Creates target-system-endpoints for a target system")
    @APIResponse(responseCode = "201", description = "Created", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = TargetSystemEndpointDTO.class, type = SchemaType.ARRAY)))
    public Response createTargetSystemEndpoints(@Parameter(name = "targetSystemId", required = true) @PathParam("targetSystemId") Long targetSystemId,
                                                @RequestBody(required = true, content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = CreateTargetSystemEndpointDTO.class, type = SchemaType.ARRAY), examples = @ExampleObject(name = "valid_target_endpoints_create", value = Examples.VALID_EXAMPLE_ENDPOINT_CREATE)))
                                                @Valid @NotNull List<CreateTargetSystemEndpointDTO> dtos,
                                                @Context UriInfo uriInfo) {
        var persisted = service.persistTargetSystemEndpointList(dtos, targetSystemId);
        Log.debugf("New target-system-endpoints created for target system %d", targetSystemId);
        return Response.status(Response.Status.CREATED).entity(mapper.mapToDTOList(persisted)).build();
    }

    /**
     * Retrieves all Target System Endpoints for a given Target System.
     *
     * @param targetSystemId The ID of the Target System.
     * @return List of TargetSystemEndpointDTO representing all endpoints for the system.
     */
    @GET
    @Path("{targetSystemId}/endpoints")
    @Operation(summary = "Returns all target-system-endpoints for a target system")
    @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = TargetSystemEndpointDTO.class, type = SchemaType.ARRAY)))
    public List<TargetSystemEndpointDTO> getAllTargetSystemEndpoints(@Parameter(name = "targetSystemId", required = true) @PathParam("targetSystemId") Long targetSystemId) {
        var endpoints = service.findAllEndpointsWithTargetSystemIdLike(targetSystemId);
        Log.debugf("Total number of target-system-endpoints: %d", endpoints.size());
        return mapper.mapToDTOList(endpoints);
    }

    /**
     * Retrieves a specific Target System Endpoint by its ID.
     * Returns HTTP 404 if the endpoint cannot be found.
     *
     * @param id The ID of the Target System Endpoint.
     * @return HTTP 200 response containing the endpoint details or 404 if not found.
     */
    @GET
    @Path("/endpoints/{id}")
    @Operation(summary = "Returns a target-system-endpoint by id")
    @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = TargetSystemEndpointDTO.class)))
    @APIResponse(responseCode = "404", description = "Not Found")
    public Response getTargetSystemEndpoint(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return service.findTargetSystemEndpointById(id)
                .map(entity -> {
                    Log.debugf("Found target-system-endpoint: %s", entity);
                    return Response.ok(mapper.mapToDTO(entity)).build();
                })
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find target-system-endpoint", "No target-system-endpoint found using id %d", id));
    }

    /**
     * Replaces an existing Target System Endpoint with new data.
     * If the endpoint does not exist, a 404 response is returned.
     *
     * @param id The ID of the Target System Endpoint to replace.
     * @param dto DTO object containing updated endpoint information.
     * @return HTTP 204 (No Content) on success, or 404 if not found.
     */
    @PUT
    @Path("/endpoints/{id}")
    @Operation(summary = "Replaces a target-system-endpoint")
    @APIResponse(responseCode = "204", description = "No Content")
    @APIResponse(responseCode = "404", description = "Not Found")
    public Response replaceTargetSystemEndpoint(@Parameter(name = "id", required = true) @PathParam("id") Long id,
                                                @Valid @NotNull TargetSystemEndpointDTO dto) {
        var entity = mapper.mapToEntity(dto);
        entity.id = id; // ensure correct target for replacement
        return service.replaceTargetSystemEndpoint(entity)
                .map(updated -> Response.noContent().build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Deletes a Target System Endpoint by its ID.
     *
     * @param id The ID of the Target System Endpoint to delete.
     */
    @DELETE
    @Path("/endpoints/{id}")
    @Operation(summary = "Deletes a target-system-endpoint")
    @APIResponse(responseCode = "204", description = "No Content")
    public void deleteTargetSystemEndpoint(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        service.deleteTargetSystemEndpointById(id);
        Log.debugf("target-system-endpoint with id %d deleted", id);
    }

    /**
     * Generates a TypeScript interface from a provided JSON schema for a specific Target System Endpoint.
     * Returns a TypeScriptGenerationResponse object containing the generated code or an error message.
     *
     * @param id The ID of the Target System Endpoint for which TypeScript should be generated.
     * @param request The TypeScriptGenerationRequest containing the JSON schema.
     * @return HTTP 200 with generated TypeScript code, or 400 if schema is invalid.
     */
    @POST
    @Path("/endpoints/{id}/generate-typescript")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Generate TypeScript interface from JSON schema for target endpoint")
    @APIResponse(
            responseCode = "200",
            description = "TypeScript interface generated successfully",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = TypeScriptGenerationResponse.class)
            )
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid JSON schema provided"
    )
    @APIResponse(
            responseCode = "404",
            description = "Endpoint not found"
    )
    public Response generateTypeScriptFromSchema(
            @Parameter(name = "id", required = true) @PathParam("id") Long id,
            @RequestBody(
                    name = "json-schema",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = TypeScriptGenerationRequest.class)
                    )
            )
            @Valid @NotNull TypeScriptGenerationRequest request) {

        try {
            String generatedTypeScript = typeScriptTypeGenerator.generate(request.jsonSchema());

            TypeScriptGenerationResponse response = new TypeScriptGenerationResponse(
                generatedTypeScript,
                null
            );

            Log.debugf("TypeScript generated successfully for target endpoint %d", id);
            return Response.ok(response).build();

        } catch (Exception e) {
            Log.warnf(e, "Failed to generate TypeScript for target endpoint %d", id);
            TypeScriptGenerationResponse errorResponse = new TypeScriptGenerationResponse(
                null,
                "Failed to generate TypeScript: " + e.getMessage()
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }
    }
}
