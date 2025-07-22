package de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.resource;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCAccessPolicy;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;

@Path("/edc-access-policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCAccessPolicyResource {

    @GET
    public List<EDCAccessPolicy> list() {
        return EDCAccessPolicy.listAll();
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") Long id) {
        EDCAccessPolicy policy = EDCAccessPolicy.findById(id);
        return policy != null
            ? Response.ok(policy).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Transactional
    public Response create(EDCAccessPolicy policy, @Context UriInfo ui) {
        policy.persist();
        URI uri = ui.getAbsolutePathBuilder().path(policy.id.toString()).build();
        return Response.created(uri).entity(policy).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, EDCAccessPolicy updated) {
        EDCAccessPolicy policy = EDCAccessPolicy.findById(id);
        if (policy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // Hier nur Verknüpfung, Permissions werden separat gepflegt
        policy.edcAsset = updated.edcAsset;
        return Response.ok(policy).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = EDCAccessPolicy.deleteById(id);
        return deleted
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }
}
