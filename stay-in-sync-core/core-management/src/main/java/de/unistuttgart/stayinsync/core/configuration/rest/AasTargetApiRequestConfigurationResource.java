package de.unistuttgart.stayinsync.core.configuration.rest;


import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.AasTargetApiRequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasTargetArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.CreateAasTargetArcDTO;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTargetApiRequestConfigurationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

@Path("/api/config/aas-target-request-configuration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AAS Target Request Configuration", description = "Endpoints for managing AAS-based Target ARCs")
public class AasTargetApiRequestConfigurationResource {

    @Inject
    AasTargetApiRequestConfigurationService aasTargetArcService;

    @Inject
    AasTargetApiRequestConfigurationMapper mapper;

    @POST
    public Response create(CreateAasTargetArcDTO dto, @Context UriInfo uriInfo) {
        AasTargetApiRequestConfiguration persistedArc = aasTargetArcService.create(dto);
        URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(persistedArc.id)).build();
        return Response.created(location).entity(mapper.mapToDto(persistedArc)).build();
    }

    @GET
    @Path("/by-transformation/{id}")
    public List<AasTargetArcDTO> getByTransformationId(@PathParam("id") Long transformationId) {
        return aasTargetArcService.findAllByTransformationId(transformationId);
    }

    @GET
    @Path("/{id}")
    public AasTargetArcDTO getById(@PathParam("id") Long id) {
        return aasTargetArcService.findById(id)
                .map(mapper::mapToDto)
                .orElseThrow(() -> new NotFoundException("AAS Target ARC with id " + id + " not found."));
    }

    @PUT
    @Path("/{id}")
    public AasTargetArcDTO update(@PathParam("id") Long id, @Valid CreateAasTargetArcDTO dto) {
        AasTargetApiRequestConfiguration updatedArc = aasTargetArcService.update(id, dto);
        return mapper.mapToDto(updatedArc);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        aasTargetArcService.delete(id);
        return Response.noContent().build();
    }
}