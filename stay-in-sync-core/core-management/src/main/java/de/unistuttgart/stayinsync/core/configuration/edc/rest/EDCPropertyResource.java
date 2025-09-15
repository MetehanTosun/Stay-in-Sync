package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCPropertyService;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/config/edcs/properties")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCPropertyResource {

    @Inject
    EDCPropertyService service;

    @GET
    public List<EDCPropertyDto> list() {
        return service.listAll();
    }

    @GET
    @Path("{id}")
    public EDCPropertyDto get(@PathParam("id") UUID id) {
        try {
            return service.findById(id);
        } catch (CustomException e) {
            throw new NotFoundException("EDCProperty " + id + " nicht gefunden");
        }
    }

    @POST
    @Transactional
    public Response create(EDCPropertyDto dto, @Context UriInfo uriInfo) {
        EDCPropertyDto created = service.create(dto);

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
    public EDCPropertyDto update(@PathParam("id") UUID id, EDCPropertyDto dto) {
        dto.setId(id);
        try {
            return service.update(id, dto);
        } catch (CustomException e) {
            throw new NotFoundException("EDCProperty " + id + " nicht gefunden");
        }
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        try {
            if (!service.delete(id)) {
                throw new NotFoundException("EDCProperty " + id + " nicht gefunden");
            }
        } catch (CustomException e) {
            throw new NotFoundException("EDCProperty " + id + " nicht gefunden: " + e.getMessage());
        }
    }
}
