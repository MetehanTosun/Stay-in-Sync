package de.unistuttgart.stayinsync.syncnode.monitor.snapshot;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphSnapshot.GraphSnapshotDTO;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Very simple in-memory store for snapshots.
 * Can later be replaced with Caffeine/Redis/etc.
 */
@ApplicationScoped
public class SnapshotStore {

    // simple TTL (seconds)
    private static final long DEFAULT_TTL_SECONDS = 3600L;

    private final Map<String, GraphSnapshotDTO> bySnapshotId = new ConcurrentHashMap<>();
    private final Map<String, GraphSnapshotDTO> latestByJobId = new ConcurrentHashMap<>();

    public void put(GraphSnapshotDTO snapshot) {
        if (snapshot == null)
            return;
        bySnapshotId.put(snapshot.getSnapshotId(), snapshot);
        if (snapshot.getJobId() != null) {
            latestByJobId.put(snapshot.getJobId(), snapshot);
        }
        evictExpired();
    }

    public Optional<GraphSnapshotDTO> getBySnapshotId(String snapshotId) {
        evictExpired();
        return Optional.ofNullable(bySnapshotId.get(snapshotId));
    }

    public Optional<GraphSnapshotDTO> getLatestByJobId(String jobId) {
        evictExpired();
        return Optional.ofNullable(latestByJobId.get(jobId));
    }

    private void evictExpired() {
        long now = Instant.now().getEpochSecond();
        bySnapshotId.values().removeIf(s -> isExpired(s, now));
        latestByJobId.values().removeIf(s -> isExpired(s, now));
    }

    private boolean isExpired(GraphSnapshotDTO s, long nowEpochSec) {
        return s.getCreatedAt() != null &&
                (nowEpochSec - s.getCreatedAt().getEpochSecond()) > DEFAULT_TTL_SECONDS;
    }
}