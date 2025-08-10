package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCPropertyMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCPropertyService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/config/edcs/properties")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCPropertyResource {

    @Inject
    EDCPropertyService service;

    @GET
    public List<EDCPropertyDto> list() {
        return service.listAll().stream()
            .map(EDCPropertyMapper::toDto)
            .collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    public EDCPropertyDto get(@PathParam("id") UUID id) {
        return service.findById(id)
            .map(EDCPropertyMapper::toDto)
            .orElseThrow(() -> new NotFoundException("EDCProperty " + id + " nicht gefunden"));
    }

    @POST
    @Transactional
    public Response create(EDCPropertyDto dto, @Context UriInfo uriInfo) {
        var entity     = EDCPropertyMapper.fromDto(dto);
        var created    = service.create(entity);
        var createdDto = EDCPropertyMapper.toDto(created);

        URI uri = uriInfo.getAbsolutePathBuilder()
                         .path(createdDto.getId().toString())
                         .build();
        return Response.created(uri)
                       .entity(createdDto)
                       .build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public EDCPropertyDto update(@PathParam("id") UUID id, EDCPropertyDto dto) {
        dto.setId(id);
        var entity = EDCPropertyMapper.fromDto(dto);
        return service.update(id, entity)
            .map(EDCPropertyMapper::toDto)
            .orElseThrow(() -> new NotFoundException("EDCProperty " + id + " nicht gefunden"));
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        if (!service.delete(id)) {
            throw new NotFoundException("EDCProperty " + id + " nicht gefunden");
        }
    }
}
