package de.unistuttgart.stayinsync.syncnode.syncjob.assets;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

/**
 * A RESTful resource providing read-only access to the asset cache.
 * <p>
 * The purpose of this API endpoint is to expose the contents of the {@link CheckResponseCacheService}
 * for diagnostic and monitoring purposes. It allows developers or administrators to inspect the
 * cached check responses associated with a specific transformation, which can be invaluable for
 * debugging synchronization issues.
 */
@Path(AssetCacheResource.ASSET_CACHE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AssetCacheResource {

    // CONSTANTS
    public static final String ASSET_CACHE_PATH = "/api/sync-node/asset-cache";
    private static final String TRANSFORMATION_ID_PARAM = "transformationId";

    /**
     * A simple, standardized structure for returning error messages as JSON.
     * Using a record provides an immutable, concise data carrier.
     */
    public record ErrorResponse(String message) {}

    private final CheckResponseCacheService cacheService;

    /**
     * Constructs the resource with its required service dependency.
     * Constructor injection is used to make dependencies explicit and to facilitate
     * easier testing by allowing mock services to be injected.
     *
     * @param cacheService The service responsible for managing the asset cache. Must not be null.
     */
    public AssetCacheResource(CheckResponseCacheService cacheService) {
        this.cacheService = Objects.requireNonNull(cacheService, "CheckResponseCacheService must not be null");
    }

    /**
     * Retrieves all cached check responses for a given transformation ID.
     * <p>
     * This endpoint searches the cache for any data associated with the provided ID.
     * If data is found, it is returned with an HTTP 200 (OK) status. If no data is
     * found, it returns an HTTP 404 (Not Found) status with a structured JSON
     * error message.
     *
     * @param transformationId The unique identifier of the transformation to look up in the cache.
     * @return A {@link Response} object containing either the cached data (on success)
     *         or a JSON error object (on failure).
     */
    @GET
    @Path("/{" + TRANSFORMATION_ID_PARAM + "}")
    public Response getResponsesForTransformation(@PathParam(TRANSFORMATION_ID_PARAM) Long transformationId) {
        return cacheService.getResponsesByTransformationId(transformationId)
                .map(data -> Response.ok(data).build())
                .orElseGet(() -> {
                    String errorMessage = String.format("No cached responses found for transformation ID: %d", transformationId);
                    ErrorResponse error = new ErrorResponse(errorMessage);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(error)
                            .build();
                });
    }
}