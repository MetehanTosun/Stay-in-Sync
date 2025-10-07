package de.unistuttgart.stayinsync.core.configuration.service.aas;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "aas-client")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AasTraversalApi {

    @GET
    @Path("shells/{aasId}")
    Response getShell(@PathParam("aasId") String aasId);

    @GET
    @Path("shells/{aasId}/submodel-refs")
    Response listSubmodelRefs(@PathParam("aasId") String aasId);

    @GET
    @Path("submodels/{submodelId}")
    Response getSubmodel(@PathParam("submodelId") String submodelId);
}
