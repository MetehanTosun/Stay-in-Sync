package de.unistuttgart.stayinsync.core.configuration.rest.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

@Path("/api/sync-node/asset-cache/")
@RegisterRestClient
public interface SyncedAssetsClient {

    @GET
    @Path("/{id}")
    Map<Long, List<String>> getById(@PathParam("id") Long id);
}
