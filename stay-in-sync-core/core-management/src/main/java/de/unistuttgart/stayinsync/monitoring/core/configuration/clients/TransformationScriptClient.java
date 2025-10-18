package de.unistuttgart.stayinsync.monitoring.core.configuration.clients;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/config/transformation")
@RegisterRestClient(configKey = "transformation-script-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
/**
 * REST client interface for accessing transformation scripts managed by the
 * core-management service.
 * <p>
 * This client communicates with the transformation configuration endpoints that
 * store
 * and serve JavaScript/TypeScript transformation scripts. It is mainly used by
 * the
 * replay and snapshot subsystems to fetch the code associated with a given
 * transformation id.
 * </p>
 *
 * <p>
 * Endpoint base path: {@code /api/config/transformation}
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
public interface TransformationScriptClient {

    /**
     * Retrieve the transformation script (JavaScript or TypeScript) for a given
     * transformation id.
     *
     * @endpoint GET /api/config/transformation/{id}/script
     * @param transformationId the unique identifier of the transformation whose
     *                         script should be fetched
     * @return a {@link TransformationScriptDTO} containing the script metadata and
     *         source code;
     *         may be {@code null} if the transformation is not found
     */
    @GET
    @Path("/{id}/script")
    TransformationScriptDTO findByTransformationId(@PathParam("id") Long transformationId);
}
