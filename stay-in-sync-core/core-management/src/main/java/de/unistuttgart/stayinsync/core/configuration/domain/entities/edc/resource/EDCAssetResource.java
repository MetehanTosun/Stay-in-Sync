package de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.resource;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCAsset;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/edc-assets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCAssetResource {

    @GET
    public List<EDCAsset> list() {
        return EDCAsset.listAll();
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") Long id) {
        EDCAsset asset = EDCAsset.findById(id);
        return asset != null
            ? Response.ok(asset).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Transactional
    public Response create(EDCAsset asset, @Context UriInfo uriInfo) {
        // Achte darauf, dass dataAddress, properties, targetSystemEndpoint und targetEDC
        // bereits als Entitäten existieren (über ihre /edc-data-addresses, /edc-properties, etc.).
        asset.persist();
        URI uri = uriInfo.getAbsolutePathBuilder()
                        .path(asset.id.toString())
                        .build();
        return Response.created(uri).entity(asset).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, EDCAsset updated) {
        EDCAsset asset = EDCAsset.findById(id);
        if (asset == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        asset.assetId               = updated.assetId;
        asset.dataAddress           = updated.dataAddress;
        asset.properties            = updated.properties;
        asset.targetSystemEndpoint  = updated.targetSystemEndpoint;
        asset.targetEDC             = updated.targetEDC;
        return Response.ok(asset).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = EDCAsset.deleteById(id);
        return deleted
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }
}
