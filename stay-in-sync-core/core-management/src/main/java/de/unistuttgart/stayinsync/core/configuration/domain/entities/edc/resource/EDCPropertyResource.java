package de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.resource;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCProperty;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/edc-properties")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCPropertyResource {

    @GET
    public List<EDCProperty> list() {
        return EDCProperty.listAll();
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") Long id) {
        EDCProperty prop = EDCProperty.findById(id);
        return prop != null
            ? Response.ok(prop).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Transactional
    public Response create(EDCProperty prop, @Context UriInfo uriInfo) {
        prop.persist();
        URI uri = uriInfo.getAbsolutePathBuilder().path(prop.id.toString()).build();
        return Response.created(uri).entity(prop).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, EDCProperty updated) {
        EDCProperty prop = EDCProperty.findById(id);
        if (prop == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        prop.description = updated.description;
        return Response.ok(prop).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = EDCProperty.deleteById(id);
        return deleted
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }
}
