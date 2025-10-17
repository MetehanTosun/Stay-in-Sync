package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncNodeClient;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Service for retrieving snapshot data related to transformations.
 * <p>
 * This service communicates with the {@link SyncNodeClient}, which connects to
 * synchronization nodes responsible for storing and managing transformation snapshots.
 * It provides methods to fetch the latest snapshot, the last five snapshots, or a snapshot by ID.
 * </p>
 * <p>
 * In case of errors during the remote calls, this service logs the error
 * and returns safe fallback values (such as {@code null} or an empty list).
 * </p>
 */
@ApplicationScoped
public class SnapshotService {

    /**
     * REST client used to interact with synchronization nodes.
     * Provides methods for querying snapshot data.
     */
    @Inject
    SyncNodeClient syncNodeClient;

    /**
     * Fetches the latest snapshot for a given transformation.
     *
     * @param transformationId the ID of the transformation
     * @return the latest {@link SnapshotDTO}, or {@code null} if the fetch fails
     */
    public SnapshotDTO getLatestSnapshot(Long transformationId) {
        try {
            return syncNodeClient.getLatest(transformationId);
        } catch (Exception e) {
            Log.error("Failed to fetch latest snapshot for transformationId=" + transformationId, e);
            return null;
        }
    }

    /**
     * Fetches up to the last five snapshots for a given transformation.
     *
     * @param transformationId the ID of the transformation
     * @return a list of up to five {@link SnapshotDTO} objects, or an empty list if the fetch fails
     */
    public List<SnapshotDTO> getLastFiveSnapshots(Long transformationId) {
        try {
            return syncNodeClient.getLastFive(transformationId);
        } catch (Exception e) {
            Log.error("Failed to fetch last five snapshots for transformationId=" + transformationId, e);
            return List.of();
        }
    }

    /**
     * Fetches a snapshot by its ID.
     *
     * @param id the ID of the snapshot
     * @return the corresponding {@link SnapshotDTO}, or {@code null} if the fetch fails
     */
    public SnapshotDTO getById(String id) {
        try {
            Log.debug("Fetching latest snapshot for id=" + id);
            return syncNodeClient.getById(id);
        } catch (Exception e) {
            Log.error("Failed to fetch snapshot with id=" + id, e);
            return null;
        }
    }

}
