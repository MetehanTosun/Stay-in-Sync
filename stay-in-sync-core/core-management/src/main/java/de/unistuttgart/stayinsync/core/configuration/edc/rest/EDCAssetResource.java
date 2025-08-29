package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
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
        return service.listAll();
    }
    
    @GET
    @Path("{id}")
    public EDCAssetDto get(@PathParam("id") UUID id) {
        return service.findById(id);
            
    }

    @POST
    @Transactional
    public Response create(final EDCAssetDto assetDto, @Context final UriInfo uriInfo) {
        final EDCAssetDto createdAsset  = service.create(assetDto);

        URI uri = uriInfo.getAbsolutePathBuilder()
                         .path(createdAsset.id().toString())
                         .build();
        return Response.created(uri)
                       .entity(createdAsset)
                       .build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public EDCAssetDto update(@PathParam("id") UUID id, final EDCAssetDto assetDto) {
        return service.update(id, assetDto);
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
