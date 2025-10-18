package de.unistuttgart.stayinsync.monitoring.core.configuration.clients;

import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client interface for interacting with the Snapshot service provided by
 * the core-sync-node.
 * <p>
 * This client is used by the core-management service to fetch snapshot data for
 * replay and debugging
 * operations. It maps directly to the endpoints exposed by
 * {@code /monitoring/snapshots} in the
 * SnapshotResource.
 * </p>
 *
 * <p>
 * Typical usage:
 * <ul>
 * <li>Retrieve a snapshot by its unique identifier using
 * {@link #byId(String)}.</li>
 * <li>Optionally fetch the latest snapshot associated with a given job or
 * transformation via {@link #latest(String)}.</li>
 * </ul>
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
@Path("/monitoring/snapshots") // Path to SnapshotResource in core-sync-node
@RegisterRestClient(configKey = "snapshot-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SnapshotClient {

    /**
     * Retrieve a specific snapshot by its unique identifier.
     *
     * @endpoint GET /monitoring/snapshots/{snapshotId}
     * @param id the snapshot identifier path parameter
     * @return the {@link SnapshotDTO} representing the snapshot; may be
     *         {@code null} if not found
     */
    @GET
    @Path("/{snapshotId}")
    SnapshotDTO byId(@PathParam("snapshotId") String id);

    /**
     * Retrieve the latest snapshot for a specific job or transformation.
     * <p>
     * This endpoint is optional and may be used to quickly access the most recent
     * snapshot created for a particular job identifier.
     * </p>
     *
     * @endpoint GET /monitoring/snapshots/latest?jobId=...
     * @param jobId identifier of the job or transformation
     * @return the most recent {@link SnapshotDTO} available for that job; may be
     *         {@code null}
     */
    @GET
    @Path("/latest")
    SnapshotDTO latest(@QueryParam("jobId") String jobId);
}