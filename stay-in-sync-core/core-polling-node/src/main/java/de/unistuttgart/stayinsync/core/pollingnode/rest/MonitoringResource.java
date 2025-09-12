package de.unistuttgart.stayinsync.core.pollingnode.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("api/monitoring/")
public class MonitoringResource {

    @GET
    public Response test() {


        return Response.ok().build();
    }
}
