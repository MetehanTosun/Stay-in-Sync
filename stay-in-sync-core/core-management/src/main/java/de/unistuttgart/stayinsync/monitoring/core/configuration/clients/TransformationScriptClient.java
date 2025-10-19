package de.unistuttgart.stayinsync.monitoring.core.configuration.clients;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/config/transformation")
@RegisterRestClient(configKey = "transformation-script-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TransformationScriptClient {

    @GET
    @Path("/{id}/script")
    TransformationScriptDTO findByTransformationId(@PathParam("id") Long transformationId);
}
