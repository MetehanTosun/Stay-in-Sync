package de.unistuttgart.stayinsync.syncnode.SnapshotManagement;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource exposing read and creation endpoints for snapshot data used by
 * the Snapshot Management & Replay feature.
 * <p>
 * A snapshot captures the state of a transformation (produced by the script
 * engine)
 * at a point in time—especially upon errors—so that users can later replay and
 * debug
 * the exact context. This resource allows:
 * <ul>
 * <li>Creating a snapshot from a
 * {@link de.unistuttgart.stayinsync.scriptengine.message.TransformationResult}.</li>
 * <li>Fetching a snapshot by its identifier.</li>
 * <li>Fetching the latest snapshot for a given transformation.</li>
 * <li>Listing recent snapshots for a transformation.</li>
 * <li>Fetching the latest snapshots across all transformations.</li>
 * </ul>
 * Endpoints are served under the base path {@code /monitoring/snapshots} and
 * return JSON.
 * </p>
 *
 * <p>
 * Note: This resource performs input validation and delegates persistence to
 * {@link SnapshotStore}. It does not perform any transformation logic itself.
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
@Path("/monitoring/snapshots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnapshotResource {

    // Backing store for persisting and retrieving snapshot DTOs.
    @Inject
    SnapshotStore store;

    // Jackson mapper used by the factory/mapper when building transport-layer DTOs.
    @Inject
    ObjectMapper om;

    /**
     * Create a snapshot from a raw {@link TransformationResult}.
     * <p>
     * This optional hook is useful for manual testing or for systems that directly
     * submit a transformation result without going through the full execution
     * wiring.
     * </p>
     *
     * @endpoint POST /monitoring/snapshots/from-result
     * @param tr the transformation result to snapshot; must not be {@code null}
     * @return {@code 201 Created} with the created {@link SnapshotDTO} on success,
     *         or {@code 400 Bad Request} if the request body is empty
     */
    @POST
    @Path("/from-result")
    public Response createFromResult(TransformationResult tr) {
        // Validate input: body must contain a TransformationResult.
        if (tr == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("empty body").build();
        // Build a snapshot DTO (assigns id, timestamp, and mapped result).
        SnapshotDTO snapshot = SnapshotFactory.fromTransformationResult(tr, om);
        // Persist the snapshot for later retrieval and replay.
        store.put(snapshot);
        // Return 201 Created and the created snapshot.
        return Response.status(Response.Status.CREATED).entity(snapshot).build();
    }

    /**
     * Fetch a snapshot by its identifier.
     *
     * @endpoint GET /monitoring/snapshots/{snapshotId}
     * @param id the snapshot identifier (path parameter)
     * @return {@code 200 OK} with the snapshot if found; otherwise
     *         {@code 404 Not Found}
     */
    @GET
    @Path("/{snapshotId}")
    public Response byId(@PathParam("snapshotId") String id) {
        // Lookup the snapshot and convert the Optional to a suitable HTTP response.
        return store.getBySnapshotId(id)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    /**
     * Fetch the latest snapshot for a specific transformation.
     *
     * @endpoint GET /monitoring/snapshots/latest?transformationId=...
     * @param transformationId the identifier of the transformation whose latest
     *                         snapshot is requested
     * @return {@code 200 OK} with the snapshot if present; {@code 404 Not Found} if
     *         none exists;
     *         or {@code 400 Bad Request} if the required query parameter is missing
     */
    @GET
    @Path("/latest")
    public Response latest(@QueryParam("transformationId") Long transformationId) {
        if (transformationId == null) {
            // Guard: transformationId is mandatory for this endpoint.
            return Response.status(Response.Status.BAD_REQUEST).entity("transformationId is required").build();
        }
        // Resolve latest snapshot and map to HTTP response.
        return store.getLatestByTransformationId(transformationId)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    /**
     * List recent snapshots for a transformation.
     *
     * @endpoint GET /monitoring/snapshots/list?transformationId=...&limit=...
     * @param transformationId the transformation identifier to filter by (required)
     * @param limit            the maximum number of results to return; defaults to
     *                         5; must be >= 1
     * @return {@code 200 OK} with a list of {@link SnapshotDTO}; or
     *         {@code 400 Bad Request}
     *         if the required query parameter is missing
     */
    @GET
    @Path("/list")
    public Response list(@QueryParam("transformationId") Long transformationId,
            @QueryParam("limit") @DefaultValue("5") int limit) {
        // Guard: transformationId is required to scope the listing.
        if (transformationId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("transformationId is required").build();
        }
        // Ensure a positive limit and return the page of results.
        return Response.ok(store.listByTransformationId(transformationId, Math.max(1, limit))).build();
    }

    /**
     * Fetch the latest snapshot per transformation across the system.
     *
     * @endpoint GET /monitoring/snapshots/latestAll
     * @return {@code 200 OK} with a map keyed by transformation id and values of
     *         {@link SnapshotDTO}
     */
    @GET
    @Path("/latestAll")
    public Response latestAll() {
        // Aggregate the latest snapshot for each known transformation.
        Map<Long, SnapshotDTO> latestSnapshots = store.getLatestByAllTransformationIds();
        // Return the aggregated map as JSON.
        return Response.ok(latestSnapshots).build();
    }

}