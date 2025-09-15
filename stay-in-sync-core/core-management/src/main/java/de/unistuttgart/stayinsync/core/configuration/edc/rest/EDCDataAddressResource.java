package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCDataAddressService;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/config/edcs/data-addresses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCDataAddressResource {

    @Inject
    EDCDataAddressService service;

    @GET
    public List<EDCDataAddressDto> list() {
        return service.listAll();
    }

    @GET @Path("{id}")
    public EDCDataAddressDto get(@PathParam("id") UUID id) {
        try {
            return service.findById(id);
        } catch (CustomException e) {
            throw new NotFoundException("DataAddress " + id + " nicht gefunden");
        }
    }

    @POST @Transactional
    public Response create(EDCDataAddressDto dto, @Context UriInfo uriInfo) {
        EDCDataAddressDto created = service.create(dto);
        
        URI uri = uriInfo.getAbsolutePathBuilder()
                         .path(created.getId().toString())
                         .build();
        return Response.created(uri)
                       .entity(created)
                       .build();
    }
}
