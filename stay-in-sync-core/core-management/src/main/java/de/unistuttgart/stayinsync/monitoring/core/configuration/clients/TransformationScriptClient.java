package de.unistuttgart.stayinsync.monitoring.core.configuration.clients;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/config/transformation")
@RegisterRestClient(configKey = "transformation-script-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TransformationScriptClient {

    @GET
    @Path("/{id}/script")
    TransformationScriptDTO findByTransformationId(@PathParam("id") Long transformationId);
}
