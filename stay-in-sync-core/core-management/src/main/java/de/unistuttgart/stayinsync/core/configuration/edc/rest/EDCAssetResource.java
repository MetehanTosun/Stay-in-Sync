package de.unistuttgart.stayinsync.core.configuration.edc.rest;



import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAssetMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCAssetService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/config/edcs/assets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "EDCAsset", description = "Verwaltet EDC‑Assets")
public class EDCAssetResource {

    @Inject
    EDCAssetService service;

    @GET
    public List<EDCAssetDto> list() {
        return service.listAll().stream()
            .map(EDCAssetMapper::toDto)
            .collect(Collectors.toList());
    }

    @GET @Path("{id}")
    public EDCAssetDto get(@PathParam("id") Long id) {
        return service.findById(id)
            .map(EDCAssetMapper::toDto)
            .orElseThrow(() -> new NotFoundException("Asset " + id + " nicht gefunden"));
    }

    @POST @Transactional
    public Response create(EDCAssetDto dto, @Context UriInfo uriInfo) {
        var entity = EDCAssetMapper.fromDto(dto);
        var created = service.createFromDto(entity);
        var createdDto = EDCAssetMapper.toDto(created);
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
public EDCAssetDto update(@PathParam("id") Long id, EDCAssetDto dto) {
    dto.setId(id);
    EDCAsset newState = EDCAssetMapper.fromDto(dto);
    java.util.Optional<EDCAsset> updated = service.update(id, newState);
    return updated
        .map(EDCAssetMapper::toDto)
        .orElseThrow(() -> new NotFoundException("Asset " + id + " nicht gefunden"));
}

    @DELETE @Path("{id}") @Transactional
    public void delete(@PathParam("id") Long id) {
        if (!service.delete(id)) {
            throw new NotFoundException("Asset " + id + " nicht gefunden");
        }
    }
}
