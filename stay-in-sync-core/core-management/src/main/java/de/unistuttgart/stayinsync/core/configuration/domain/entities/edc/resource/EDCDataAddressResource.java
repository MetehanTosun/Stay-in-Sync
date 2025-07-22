package de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.resource;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCDataAddress;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/edc-data-addresses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCDataAddressResource {

    @GET
    public List<EDCDataAddress> list() {
        return EDCDataAddress.listAll();
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") Long id) {
        EDCDataAddress da = EDCDataAddress.findById(id);
        return da != null
            ? Response.ok(da).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Transactional
    public Response create(EDCDataAddress da, @Context UriInfo uriInfo) {
        da.persist();
        URI uri = uriInfo.getAbsolutePathBuilder().path(da.id.toString()).build();
        return Response.created(uri).entity(da).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, EDCDataAddress updated) {
        EDCDataAddress da = EDCDataAddress.findById(id);
        if (da == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        da.jsonLDType         = updated.jsonLDType;
        da.type               = updated.type;
        da.baseURL            = updated.baseURL;
        da.proxyPath          = updated.proxyPath;
        da.proxyQueryParams   = updated.proxyQueryParams;
        return Response.ok(da).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = EDCDataAddress.deleteById(id);
        return deleted
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }
}
