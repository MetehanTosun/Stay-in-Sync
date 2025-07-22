package de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.resource;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCAccessPolicyPermission;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;

@Path("/edc-access-policy-permissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCAccessPolicyPermissionResource {

    @GET
    public List<EDCAccessPolicyPermission> list() {
        return EDCAccessPolicyPermission.listAll();
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") Long id) {
        EDCAccessPolicyPermission perm = EDCAccessPolicyPermission.findById(id);
        return perm != null
            ? Response.ok(perm).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Transactional
    public Response create(EDCAccessPolicyPermission perm, @Context UriInfo ui) {
        perm.persist();
        URI uri = ui.getAbsolutePathBuilder().path(perm.id.toString()).build();
        return Response.created(uri).entity(perm).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, EDCAccessPolicyPermission updated) {
        EDCAccessPolicyPermission perm = EDCAccessPolicyPermission.findById(id);
        if (perm == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        perm.action               = updated.action;
        perm.constraintLeftOperand  = updated.constraintLeftOperand;
        perm.constraintOperator     = updated.constraintOperator;
        perm.constraintRightOperand = updated.constraintRightOperand;
        perm.edcAccessPolicy      = updated.edcAccessPolicy;
        return Response.ok(perm).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = EDCAccessPolicyPermission.deleteById(id);
        return deleted
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }
}
