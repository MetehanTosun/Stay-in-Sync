package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAssetMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCAssetService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/config/edcs/assets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCAssetResource {

    @Inject
    EDCAssetService service;

    @GET
    public List<EDCAssetDto> list() {
        return service.listAll().stream()
            .map(EDCAssetMapper::toDto)
            .collect(Collectors.toList());
    }
    
    @GET
    @Path("{id}")
    public EDCAssetDto get(@PathParam("id") UUID id) {
        return service.findById(id)
            .map(EDCAssetMapper::toDto)
            .orElseThrow(() -> new NotFoundException("Asset " + id + " nicht gefunden"));
            
    }

    @POST
    @Transactional
    public Response create(EDCAssetDto dto, @Context UriInfo uriInfo) {
        // dto.getId() kann null sein – neue UUID wird im Entity per @PrePersist gesetzt
        EDCAsset entity   = EDCAssetMapper.fromDto(dto);
        EDCAsset created  = service.create(entity);
        EDCAssetDto result = EDCAssetMapper.toDto(created);
        
        URI uri = uriInfo.getAbsolutePathBuilder()
                         .path(result.getId().toString())
                         .build();
        return Response.created(uri)
                       .entity(result)
                       .build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public EDCAssetDto update(@PathParam("id") UUID id, EDCAssetDto dto) {
        dto.setId(id);
        EDCAsset newState = EDCAssetMapper.fromDto(dto);
        return service.update(id, newState)
            .map(EDCAssetMapper::toDto)
            .orElseThrow(() -> new NotFoundException("Asset " + id + " nicht gefunden"));
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        if (!service.delete(id)) {
            throw new NotFoundException("Asset " + id + " nicht gefunden");
        }
    }
}
