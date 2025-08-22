package de.unistuttgart.stayinsync.pollingnode.rest;

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
