package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemApiRequestConfigurationFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.GetRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemApiRequestConfigurationService;
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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/source-system/")
public class RequestConfigurationResource {
    @Inject
    SourceSystemApiRequestConfigurationService sourceSystemApiRequestConfigurationService;

    @Inject
    SourceSystemApiRequestConfigurationFullUpdateMapper fullUpdateMapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a valid api request configuration for a specified source system endpoint")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created api-request-configuration",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid api request configuration passed in (or no request body found)"
    )
    @Path("endpoint/{endpointId}/request-configuration/")
    public Response createRequestConfiguration(
            @PathParam("endpointId") Long endpointId,
            @RequestBody(
                    name = "api-request-configuration",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = CreateRequestConfigurationDTO.class),
                            examples = @ExampleObject(name = "valid request config", value = Examples.VALID_EXAMPLE_REQUEST_CONFIGURATION_CREATE)
                    )
            )
            @Valid @NotNull CreateRequestConfigurationDTO sourceSystemApiRequestConfigurationDTO,
            @Context UriInfo uriInfo) {
        var persistedApiRequestConfiguration = this.sourceSystemApiRequestConfigurationService.persistApiRequestConfiguration(sourceSystemApiRequestConfigurationDTO, endpointId);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedApiRequestConfiguration.id));
        Log.infof("New api-request-configuration created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }

    @GET
    @Path("/endpoint/{endpointId}/request-configuration/")
    @Operation(summary = "Returns the api-request-configurations for the specified endpoint from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all api-request-configurations for the specified source-system",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = CreateRequestConfigurationDTO.class, type = SchemaType.ARRAY)
            )
    )
    public List<GetRequestConfigurationDTO> getAllSourceSystemRequestConfigurationsByEndpointId(@Parameter(name = "source_system_filter", description = "An optional filter parameter to filter results by source system id") @PathParam("endpointId") Long endpointId) {
        var apiRequestConfigurations = sourceSystemApiRequestConfigurationService.findAllRequestConfigurationsByEndpointId(endpointId);

        Log.infof("Total number of api request configurations by endpoint: %d", apiRequestConfigurations.size());

        return fullUpdateMapper.mapToDTOList(apiRequestConfigurations);
    }

    @GET
    @Path("{sourceSystemId}/request-configuration/")
    @Operation(summary = "Returns all the api-request-configurations for the specified source-system from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all api-request-configurations for the specified source-system",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = GetRequestConfigurationDTO.class, type = SchemaType.ARRAY)
            )
    )
    public List<GetRequestConfigurationDTO> getAllSourceSystemRequestConfigurationsBySourceSystemId(@Parameter(name = "source_system_filter", description = "An optional filter parameter to filter results by source system id") @PathParam("sourceSystemId") Long sourceSystemId) {
        var apiRequestConfigurations = sourceSystemApiRequestConfigurationService.findAllRequestConfigurationsWithSourceSystemIdLike(sourceSystemId);

        Log.infof("Total number of api request configurations by source-system: %d", apiRequestConfigurations.size());

        return fullUpdateMapper.mapToDTOList(apiRequestConfigurations);
    }


    @GET
    @Path("endpoint/request-configuration/{id}")
    @Operation(summary = "Returns a api-request-configuration for a given identifier")
    @APIResponse(
            responseCode = "200",
            description = "Gets a api-request-configuration for a given id",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = CreateRequestConfigurationDTO.class),
                    examples = @ExampleObject(name = "api-request-configuration", value = Examples.VALID_EXAMPLE_SYNCJOB)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The api-request-configuration is not found for a given identifier"
    )
    public Response getSourceSystemEndpoint(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.sourceSystemApiRequestConfigurationService.findApiRequestConfigurationById(id)
                .map(sourceSystemApiRequestConfiguration -> {
                    Log.infof("Found api-request-configuration: %s", sourceSystemApiRequestConfiguration);
                    return Response.ok(fullUpdateMapper.mapToDTO(sourceSystemApiRequestConfiguration)).build();
                })
                .orElseThrow(() -> {
                    Log.warnf("No api-request-configuration found using id %d", id);
                    return new CoreManagementException(Response.Status.NOT_FOUND, "Unable to find api-request-configuration", "No api-request-configuration found using id %d", id);
                });
    }

    @DELETE
    @Operation(summary = "Deletes an existing api-request-configuration")
    @APIResponse(
            responseCode = "204",
            description = "Delete a api-request-configuration"
    )
    @Path("endpoint/request-configuration/{id}")
    public void deleteRequestConfigurationById(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.sourceSystemApiRequestConfigurationService.deleteApiRequestConfigurationById(id);
        Log.infof("api-request-configuration with id %d deleted ", id);
    }

    @PUT
    @Path("endpoint/request-configuration/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates an existing api-request-configuration by replacing it with the passed-in api-request-configuration")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the api-request-configuration"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid api-request-configuration passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No api-request-configuration found"
    )
    public Response fullyUpdateRequestConfiguration(@Parameter(name = "id", required = true)
                                                    @RequestBody(
                                                            name = "api-request-configuration",
                                                            required = true,
                                                            content = @Content(
                                                                    mediaType = APPLICATION_JSON,
                                                                    schema = @Schema(implementation = CreateRequestConfigurationDTO.class),
                                                                    examples = @ExampleObject(name = "valid_sync_job", value = Examples.VALID_EXAMPLE_SYNCJOB)
                                                            )
                                                    )
                                                    @PathParam("id") Long id, @Valid @NotNull CreateRequestConfigurationDTO SourceSystemApiRequestConfigurationDTO) {

        return this.sourceSystemApiRequestConfigurationService.replaceApiRequestConfiguration(SourceSystemApiRequestConfigurationDTO)
                .map(updatedSourceSystemEndpoint -> {
                    Log.infof("api-request-configuration replaced with new values %s", updatedSourceSystemEndpoint);
                    return Response.noContent().build();
                })
                .orElseGet(() -> {
                    Log.infof("No api-request-configuration found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }
}
