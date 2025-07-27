package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;

@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCResource {

    @Inject
    public EDCService service;

    @GET
    public List<EDCDto> list() {
        return service.listAll().stream()
            .map(EDCMapper::toDto)
            .toList();
    }

    @GET @Path("{id}")
    public EDCDto get(@PathParam("id") Long id) {
        return service.findById(id)
            .map(EDCMapper::toDto)
            .orElseThrow(NotFoundException::new);
    }

    @POST @Transactional
    public Response create(@Valid EDCDto dto, @Context UriInfo uriInfo) {
        var edc = service.create(EDCMapper.fromDto(dto));
        var created = EDCMapper.toDto(edc);
        URI uri = uriInfo.getAbsolutePathBuilder().path(created.getId().toString()).build();
        return Response.created(uri).entity(created).build();
    }

    @PUT @Path("{id}") @Transactional
    public EDCDto update(@PathParam("id") Long id, @Valid EDCDto dto) {
        dto.setId(id);
        return service.update(id, EDCMapper.fromDto(dto))
            .map(EDCMapper::toDto)
            .orElseThrow(NotFoundException::new);
    }

    @DELETE @Path("{id}") @Transactional
    public void delete(@PathParam("id") Long id) {
        if (!service.delete(id)) throw new NotFoundException();
    }
}
