package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemDto;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemEndpointDto;
import java.util.List;
import java.util.stream.Collectors;
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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.MultipartForm;
import java.io.IOException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/source-systems")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SourceSystemResource {

    @Inject
    SourceSystemService ssService;

    @Inject
    SourceSystemEndpointService endpointService;

    @Inject
    SourceSystemEndpointMapper endpointMapper;

    @Inject
    SourceSystemMapper sourceSystemMapper;

    @GET
    @Operation(summary = "Returns all source systems")
    @APIResponse(responseCode = "200", description = "List of all source systems", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = SourceSystemDto.class)))
    public List<SourceSystemDto> getAllSs() {
        return ssService.findAllSourceSystems().stream()
                .map(sourceSystemMapper::toDto)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a source system by its ID")
    @APIResponse(responseCode = "200", description = "The found source system", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystemDto.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response getSsById(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        var found = ssService.findSourceSystemById(id)
                .orElseThrow(
                        () -> new CoreManagementWebException(
                                Response.Status.NOT_FOUND,
                                "Source system not found",
                                "No source system found with id %d", id));
        Log.debugf("Found source system: %s", found);
        SourceSystemDto dto = sourceSystemMapper.toDto(found);
        return Response.ok(dto).build();
    }

    @POST
    @Operation(summary = "Creates a new source system")
    @APIResponse(responseCode = "201", description = "The URI of the created source system", headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class)))
    @APIResponse(responseCode = "400", description = "Invalid source system passed in")
    public Response createSs(
            @RequestBody(name = "source-system", required = true, content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystemDto.class), examples = @ExampleObject(name = "valid_source_system", value = "{\"name\":\"Sensor A\",\"description\":\"Raumtemperatur\",\"endpointUrl\":\"http://localhost:8080\"}"))) @Valid @NotNull SourceSystemDto inputDto,
            @Context UriInfo uriInfo) {
        var entity = sourceSystemMapper.toEntity(inputDto);
        ssService.createSourceSystem(entity);
        SourceSystemDto result = sourceSystemMapper.toDto(entity);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(result.id()));
        Log.debugf("New source system created with URI %s", builder.build().toString());
        return Response.created(builder.build()).entity(result).build();
        // TODO: Throw Exception in case of invalid source system: we need to know how
        // the source system looks like first(final model)
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Fully updates an existing source system")
    @APIResponse(responseCode = "200", description = "The updated source system", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystemDto.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response updateSs(@Parameter(name = "id", required = true) @PathParam("id") Long id,
                             @Valid @NotNull SourceSystemDto inputDto) {
        var entity = sourceSystemMapper.toEntity(inputDto);
        entity.id = id;
        var updatedEntity = ssService.updateSourceSystem(entity)
                .orElseThrow(() -> new CoreManagementWebException(
                        Response.Status.NOT_FOUND,
                        "Source system not found",
                        "No source system found with id %d", id));
        SourceSystemDto dto = sourceSystemMapper.toDto(updatedEntity);
        return Response.ok(dto).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes a source system by its ID")
    @APIResponse(responseCode = "204", description = "Source system deleted")
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response deleteSs(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        if (!ssService.deleteSourceSystemById(id)) {
            throw new CoreManagementWebException(
                    Response.Status.NOT_FOUND,
                    "Source system not found",
                    "No source system found with id %d", id);
        }
        return Response.noContent().build();
    }

    /**
     * Upload an OpenAPI specification file for the given SourceSystem.
     *
     * @param sourceId the ID of the SourceSystem
     * @param form     the multipart form containing the file or URL
     * @return 204 No Content on success, 400/500 on error
     */
    @POST
    @Path("/{id}/upload-openapi")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadOpenApi(
            @PathParam("id") Long sourceId,
            @MultipartForm OpenApiUploadForm form) {

        // Option 1: URL provided
        if (form.openApiSpecUrl != null && !form.openApiSpecUrl.isBlank()) {
            ssService.updateOpenApiSpecUrl(sourceId, form.openApiSpecUrl);
            return Response.noContent().build();
        }

        // Option 2: File upload
        if (form.file == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Either 'url' or 'file' must be provided")
                    .build();
        }

        try {
            String spec = new String(form.uploadedFileBytes(), java.nio.charset.StandardCharsets.UTF_8);
            ssService.updateOpenApiSpec(sourceId, spec);
            return Response.noContent().build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to read uploaded file: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/{id}/endpoints")
    @Operation(summary = "List all endpoints for a source system")
    @APIResponse(responseCode = "200", description = "List of endpoints", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = SourceSystemEndpointDto.class)))
    public List<SourceSystemEndpointDto> listEndpoints(
            @Parameter(name = "id", required = true) @PathParam("id") Long sourceId) {
        return endpointService.listBySourceId(sourceId).stream()
                .map(endpointMapper::toDto)
                .collect(Collectors.toList());
    }

    @POST
    @Path("/{id}/endpoints/{eid}/extract")
    @Operation(summary = "Generate JSON schema for one endpoint")
    @APIResponse(responseCode = "200", description = "Updated endpoint with schema", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystemEndpointDto.class)))
    public SourceSystemEndpointDto extractSchema(
            @Parameter(name = "id", required = true) @PathParam("id") Long sourceId,
            @Parameter(name = "eid", required = true) @PathParam("eid") Long endpointId) {
        var updated = endpointService.extractSchema(endpointId);
        return endpointMapper.toDto(updated);
    }
}
