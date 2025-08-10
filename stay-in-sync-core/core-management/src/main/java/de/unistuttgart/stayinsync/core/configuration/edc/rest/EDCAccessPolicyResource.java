package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAccessPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAccessPolicyMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCAccessPolicyService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/config/edcs/access-policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "EDCAccessPolicy", description = "Verwaltet EDC-Access-Policies")
public class EDCAccessPolicyResource {

    @Inject
    EDCAccessPolicyService service;

    @GET
    public List<EDCAccessPolicyDto> list() {
        return service.listAll().stream()
            .map(EDCAccessPolicyMapper::toDto)
            .collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    public EDCAccessPolicyDto get(@PathParam("id") UUID id) {
        return service.findById(id)
            .map(EDCAccessPolicyMapper::toDto)
            .orElseThrow(() -> new NotFoundException("AccessPolicy " + id + " nicht gefunden"));
    }

    @POST
    @Transactional
    public Response create(EDCAccessPolicyDto dto, @Context UriInfo uriInfo) {
        var entity     = EDCAccessPolicyMapper.fromDto(dto);
        var created    = service.create(entity);
        var createdDto = EDCAccessPolicyMapper.toDto(created);
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
    public EDCAccessPolicyDto update(@PathParam("id") UUID id, EDCAccessPolicyDto dto) {
        dto.setId(id);
        var newState = EDCAccessPolicyMapper.fromDto(dto);
        return service.update(id, newState)
            .map(EDCAccessPolicyMapper::toDto)
            .orElseThrow(() -> new NotFoundException("AccessPolicy " + id + " nicht gefunden"));
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        if (!service.delete(id)) {
            throw new NotFoundException("AccessPolicy " + id + " nicht gefunden");
        }
    }
}
