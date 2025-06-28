package de.unistuttgart.stayinsync.core.configuration.rest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.MultipartForm;

import jakarta.ws.rs.core.MediaType;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemForm;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemJsonDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.DiscoveredEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemDto;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemEndpointDto;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemType;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/source-systems")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SourceSystemResource {

    @Inject SourceSystemService ssService;
    @Inject SourceSystemEndpointService endpointService;
    @Inject SourceSystemEndpointMapper endpointMapper;
    @Inject SourceSystemMapper sourceSystemMapper;

    @GET
    @Operation(summary = "Returns all source systems")
    @APIResponse(responseCode = "200", description = "List of all source systems",
      content = @Content(mediaType = APPLICATION_JSON,
                         schema = @Schema(type = SchemaType.ARRAY, implementation = SourceSystemDto.class)))
    public List<SourceSystemDto> getAllSs() {
        return ssService.findAllSourceSystems().stream()
                        .map(sourceSystemMapper::toDto)
                        .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a source system by its ID")
    @APIResponse(responseCode = "200", description = "The found source system",
      content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystemDto.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response getSsById(@PathParam("id") Long id) {
        var entity = ssService.findSourceSystemById(id)
            .orElseThrow(() -> new CoreManagementWebException(
                Response.Status.NOT_FOUND,
                "Source system not found",
                "No source system found with id %d", id));
        return Response.ok(sourceSystemMapper.toDto(entity)).build();
    }

    // JSON-only create
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a new source system (JSON only)")
    @APIResponse(responseCode = "201", description = "Source system created",
      headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class)))
    public Response createJson(
        @Valid @NotNull CreateSourceSystemJsonDTO createDto,
        @Context UriInfo uriInfo
    ) {
        // 1) REST-DTO → Entity
        var entity = sourceSystemMapper.toEntity(createDto);
        ssService.createSourceSystem(entity);

        // 2) Optional: OpenAPI-Spec
        if (entity.getType() == SourceSystemType.REST_OPENAPI) {
            if (createDto.openApiSpec() != null && !createDto.openApiSpec().isBlank()) {
                ssService.updateOpenApiSpec(entity.id, createDto.openApiSpec());
            } else if (createDto.openApiSpecUrl() != null && !createDto.openApiSpecUrl().isBlank()) {
                ssService.updateOpenApiSpecUrl(entity.id, createDto.openApiSpecUrl());
            }
        }

        // 3) Entity → REST-DTO + Location
        var result = sourceSystemMapper.toDto(entity);
        URI location = uriInfo.getAbsolutePathBuilder()
                              .path(String.valueOf(result.id()))
                              .build();
        return Response.created(location).entity(result).type(MediaType.APPLICATION_JSON).build();
    }
    // Multipart create
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @Operation(summary = "Creates a new source system (multipart/form-data + optional OpenAPI)")
    @APIResponse(responseCode = "201", description = "Source system created",
        headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class)))
    public Response createMultipart(
        @MultipartForm CreateSourceSystemForm form,
        @Context UriInfo uriInfo
    ) throws IOException {
        // 1) Form → Entity
        SourceSystem entity = sourceSystemMapper.toEntity(form);
        ssService.createSourceSystem(entity);

        // 2) OpenAPI-Spec nachreichen, falls gewünscht
        if (SourceSystemType.REST_OPENAPI.equals(form.type)) {
            if (form.openApiSpecUrl != null && !form.openApiSpecUrl.isBlank()) {
                ssService.updateOpenApiSpecUrl(entity.id, form.openApiSpecUrl);
            } else if (form.file != null) {
                String spec = new String(form.uploadedFileBytes(), StandardCharsets.UTF_8);
                ssService.updateOpenApiSpec(entity.id, spec);
            }
        }

        // 3) Entity → REST-DTO
        SourceSystemDto resultDto = sourceSystemMapper.toDto(entity);
        // 4) Location-Header
        URI location = uriInfo.getAbsolutePathBuilder()
                              .path(resultDto.id().toString())
                              .build();
        return Response.created(location)
                       .entity(resultDto)
                       .build();
    }



    @PUT
    @Path("/{id}")
    @Operation(summary = "Fully updates an existing source system")
    @APIResponse(responseCode = "200", description = "The updated source system",
      content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystemDto.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response updateSs(@PathParam("id") Long id, @Valid @NotNull SourceSystemDto dto) {
        var entity = sourceSystemMapper.toEntity(dto);
        entity.id = id;
        var updated = ssService.updateSourceSystem(entity)
            .orElseThrow(() -> new CoreManagementWebException(
                Response.Status.NOT_FOUND,
                "Source system not found",
                "No source system found with id %d", id));
        return Response.ok(sourceSystemMapper.toDto(updated)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes a source system by its ID")
    @APIResponse(responseCode = "204", description = "Source system deleted")
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response deleteSs(@PathParam("id") Long id) {
        if (!ssService.deleteSourceSystemById(id)) {
            throw new CoreManagementWebException(
                Response.Status.NOT_FOUND,
                "Source system not found",
                "No source system found with id %d", id);
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/endpoints")
    @Operation(summary = "List all endpoints for a source system")
    @APIResponse(responseCode = "200", description = "List of endpoints",
      content = @Content(mediaType = APPLICATION_JSON,
                         schema = @Schema(type = SchemaType.ARRAY, implementation = SourceSystemEndpointDto.class)))
    public List<SourceSystemEndpointDto> listEndpoints(@PathParam("id") Long sourceId) {
        return endpointService.listBySourceId(sourceId)
                              .stream()
                              .map(endpointMapper::toDto)
                              .collect(Collectors.toList());
    }

    @POST
    @Path("/{id}/endpoints")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Create a new endpoint for a source system")
    @APIResponse(responseCode = "201", description = "Endpoint created",
      headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class)))
    public Response createEndpoint(
        @PathParam("id") Long sourceId,
        @Valid @NotNull SourceSystemEndpointDto input,
        @Context UriInfo uriInfo
    ) {
        var entity = endpointService.createEndpoint(sourceId, input.endpointPath(), input.httpRequestType());
        var dto = endpointMapper.toDto(entity);
        URI location = uriInfo.getAbsolutePathBuilder()
                              .path(dto.id().toString())
                              .build();
        return Response.created(location).entity(dto).build();
    }

    @GET
    @Path("/{id}/discover")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Discover endpoints from OpenAPI spec")
    @APIResponse(responseCode = "200", description = "List of discovered endpoints",
      content = @Content(mediaType = APPLICATION_JSON,
                         schema = @Schema(type = SchemaType.ARRAY, implementation = DiscoveredEndpoint.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response discoverEndpoints(@PathParam("id") Long sourceId) {
        var list = endpointService.discoverAllEndpoints(sourceId);
        return Response.ok(list).build();
    }
}