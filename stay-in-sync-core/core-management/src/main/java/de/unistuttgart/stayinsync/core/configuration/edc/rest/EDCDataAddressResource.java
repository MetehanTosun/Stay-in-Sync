package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCDataAddressMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCDataAddressService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/config/edcs/data-addresses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCDataAddressResource {

    @Inject
    EDCDataAddressService service;

    @GET
    public List<EDCDataAddressDto> list() {
        return service.listAll().stream()
            .map(EDCDataAddressMapper::toDto)
            .collect(Collectors.toList());
    }

    @GET @Path("{id}")
    public EDCDataAddressDto get(@PathParam("id") Long id) {
        return service.findById(id)
            .map(EDCDataAddressMapper::toDto)
            .orElseThrow(() -> new NotFoundException("DataAddress " + id + " nicht gefunden"));
    }

    @POST @Transactional
    public Response create(EDCDataAddressDto dto, @Context UriInfo uriInfo) {
        var entity = EDCDataAddressMapper.fromDto(dto);
        var created = service.create(entity);
        var createdDto = EDCDataAddressMapper.toDto(created);
        URI uri = uriInfo.getAbsolutePathBuilder()
                         .path(createdDto.getId().toString())
                         .build();
        return Response.created(uri)
                       .entity(createdDto)
                       .build();
    }
}
