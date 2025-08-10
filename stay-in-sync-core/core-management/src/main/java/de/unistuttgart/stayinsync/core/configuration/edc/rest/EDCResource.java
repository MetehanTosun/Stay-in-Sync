package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCInstanceMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCResource {

    @Inject
    public EDCService service;

    @GET
    public List<EDCInstanceDto> list() {
        return service.listAll().stream()
            .map(EDCInstanceMapper::toDto)
            .toList();
    }

    @GET 
    @Path("{id}")
    public EDCInstanceDto get(@PathParam("id") UUID id) {
        return service.findById(id)
            .map(EDCInstanceMapper::toDto)
            .orElseThrow(NotFoundException::new);
    }

    @POST 
    @Transactional
    public Response create(@Valid EDCInstanceDto dto, @Context UriInfo uriInfo) {
        // dto.id sollte hier null sein
        var edc = service.create(EDCInstanceMapper.fromDto(dto));
        var created = EDCInstanceMapper.toDto(edc);
        URI uri = uriInfo.getAbsolutePathBuilder()
                         .path(created.getId().toString())
                         .build();
        return Response.created(uri)
                       .entity(created)
                       .build();
    }

    @PUT 
    @Path("{id}")
    @Transactional
    public EDCInstanceDto update(@PathParam("id") UUID id, @Valid EDCInstanceDto dto) {
        dto.setId(id);
        return service.update(id, EDCInstanceMapper.fromDto(dto))
            .map(EDCInstanceMapper::toDto)
            .orElseThrow(NotFoundException::new);
    }

    @DELETE 
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        if (!service.delete(id)) {
            throw new NotFoundException();
        }
    }
}
