package de.unistuttgart.stayinsync.syncnode.logic_engine;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * A REST client interface to communicate with the central, in-memory State-Service.
 * This client is used to fetch and save transformation rule snapshots.
 */
@Path("/snapshot")
@RegisterRestClient(configKey = "state-service-api")
public interface StateServiceClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    String getSnapshot(@PathParam("id") long transformationId);

    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void saveSnapshot(@PathParam("id") long transformationId, String snapshotJson);
}
