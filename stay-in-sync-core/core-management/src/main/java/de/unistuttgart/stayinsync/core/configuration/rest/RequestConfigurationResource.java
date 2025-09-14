package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSourceApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.AasApiRequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemApiRequestConfigurationFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.GetRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasArcDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemApiRequestConfigurationService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasApiRequestConfigurationService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/source-system/")
public class RequestConfigurationResource {
    @Inject
    SourceSystemApiRequestConfigurationService sourceSystemApiRequestConfigurationService;

    @Inject
    SourceSystemApiRequestConfigurationFullUpdateMapper fullUpdateMapper;

    @Inject
    AasApiRequestConfigurationMapper aasArcMapper;

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
    public Response createSourceSystemArc(
            @PathParam("endpointId") Long endpointId,
            @RequestBody(
                    name = "api-request-configuration",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = CreateSourceArcDTO.class)
                    )
            )
            @Valid CreateSourceArcDTO arcDto,
            @Context UriInfo uriInfo) {
        var persistedApiRequestConfiguration = this.sourceSystemApiRequestConfigurationService.create(arcDto, endpointId);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedApiRequestConfiguration.id));

        GetRequestConfigurationDTO responseDto = fullUpdateMapper.mapToDTOGet(persistedApiRequestConfiguration);
        Log.debugf("New api-request-configuration created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).entity(responseDto).build();
    }

    /*@POST
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
        Log.debugf("New api-request-configuration created with URI  %s", builder.build().toString());

        return Response.created(builder.build()).build();
    }
*/

    @POST
    @Path("/request-configuration/by-source-system-names")
    public Response getArcsBySourceSystemNames(Set<String> sourceSystemNames) {
        if (sourceSystemNames == null || sourceSystemNames.isEmpty()) {
            return Response.ok(Map.of()).build();
        }

        Map<String, List<Object>> groupedArcs = new HashMap<>();

        List<Object[]> restResults = SourceSystemApiRequestConfiguration.findArcsGroupedBySourceSystemName(sourceSystemNames);
        restResults.forEach(row ->{
            String systemName = (String) row[0];
            GetRequestConfigurationDTO arcDto = fullUpdateMapper.mapToDTOGet((SourceSystemApiRequestConfiguration) row[1]);
            groupedArcs.computeIfAbsent(systemName, k -> new ArrayList<>()).add(arcDto);
        });

        List<AasSourceApiRequestConfiguration> aasResults = AasSourceApiRequestConfiguration.list("sourceSystem.name in ?1", sourceSystemNames);

        aasResults.forEach(aasArc -> {
            String systemName = aasArc.sourceSystem.name;
            AasArcDTO aasArcDTO = aasArcMapper.mapToDto(aasArc);
            groupedArcs.computeIfAbsent(systemName, k -> new ArrayList<>()).add(aasArcDTO);
        });

        sourceSystemNames.forEach(name -> groupedArcs.putIfAbsent(name, List.of()));

        return Response.ok(groupedArcs).build();
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

        Log.debugf("Total number of api request configurations by endpoint: %d", apiRequestConfigurations.size());

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

        Log.debugf("Total number of api request configurations by source-system: %d", apiRequestConfigurations.size());

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
                    schema = @Schema(implementation = CreateRequestConfigurationDTO.class)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The api-request-configuration is not found for a given identifier"
    )
    public Response getSourceSystemApiRequestConfiguration(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return Response.ok(fullUpdateMapper.mapToDTOGet(this.sourceSystemApiRequestConfigurationService.findApiRequestConfigurationById(id))).build();
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
        Log.debugf("api-request-configuration with id %d deleted ", id);
    }

    @PUT
    @Path("endpoint/request-configuration/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Fully updates an existing Source ARC.")
    @APIResponse(responseCode = "200", description = "ARC updated successfully. The updated ARC is returned.",
            content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = GetRequestConfigurationDTO.class)))
    @APIResponse(responseCode = "400", description = "Invalid data provided.")
    @APIResponse(responseCode = "404", description = "No ARC found with the given ID.")
    public Response updateSourceSystemArc(@PathParam("id") Long id,
                                          @RequestBody(
                                                  name = "api-request-configuration",
                                                  required = true,
                                                  content = @Content(
                                                          mediaType = APPLICATION_JSON,
                                                          schema = @Schema(implementation = CreateSourceArcDTO.class)
                                                  )
                                          ) @Valid CreateSourceArcDTO arcDto) {

        return this.sourceSystemApiRequestConfigurationService.update(id, arcDto)
                .map(updatedArcDto -> Response.ok(updatedArcDto).build())
                .orElseGet(() -> {
                    Log.debugf("Update failed: No api-request-configuration found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }
}
