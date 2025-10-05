package de.unistuttgart.stayinsync.core.configuration.rest.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/extensions")
@RegisterRestClient
public interface SyncedAssetsClient {

    @GET
    void getById(@QueryParam("id") String id);
}
