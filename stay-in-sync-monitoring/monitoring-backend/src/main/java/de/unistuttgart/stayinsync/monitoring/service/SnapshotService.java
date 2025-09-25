package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncNodeClient;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SnapshotService {

    @Inject
    SyncNodeClient syncNodeClient;

    public SnapshotDTO getLatestSnapshot(Long transformationId) {
        try {
            return syncNodeClient.getLatest(transformationId);
        } catch (Exception e) {
            Log.error("Failed to fetch latest snapshot for transformationId=" + transformationId, e);
            return null;
        }
    }

    public List<SnapshotDTO> getLastFiveSnapshots(Long transformationId) {
        try {
            return syncNodeClient.getLastFive(transformationId);
        } catch (Exception e) {
            Log.error("Failed to fetch last five snapshots for transformationId=" + transformationId, e);
            return List.of();
        }
    }

    public SnapshotDTO getById(Long id) {
        try {
            return syncNodeClient.getById(id);
        } catch (Exception e) {
            Log.error("Failed to fetch snapshot with id=" + id, e);
            return null;
        }
    }

}

