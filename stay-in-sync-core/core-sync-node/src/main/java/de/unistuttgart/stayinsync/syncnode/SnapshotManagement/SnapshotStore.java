package de.unistuttgart.stayinsync.syncnode.SnapshotManagement;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.TransformationResultDTO;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SnapshotStore {

    // 24h TTL in seconds
    private static final long DEFAULT_TTL_SECONDS = 24 * 60 * 60;
    // keep last N per transformation (can be 1 if we only want the latest)
    private static final int HISTORY_LIMIT = 5;

    private final Map<String, SnapshotDTO> bySnapshotId = new ConcurrentHashMap<>();
    private final Map<Long, SnapshotDTO> latestByTransformationId = new ConcurrentHashMap<>();
    private final Map<Long, Deque<SnapshotDTO>> historyByTransformationId = new ConcurrentHashMap<>();

    public void put(SnapshotDTO snapshot) {
        if (snapshot == null)
            return;
        bySnapshotId.put(snapshot.getSnapshotId(), snapshot);

        TransformationResultDTO transformationResult = snapshot.getTransformationResult();
        if (transformationResult != null && transformationResult.getTransformationId() != null) {
            Long transformationId = transformationResult.getTransformationId();
            latestByTransformationId.put(transformationId, snapshot);

            historyByTransformationId.computeIfAbsent(transformationId, k -> new ArrayDeque<>(HISTORY_LIMIT));
            Deque<SnapshotDTO> dq = historyByTransformationId.get(transformationId);
            dq.addFirst(snapshot);
            while (dq.size() > HISTORY_LIMIT)
                dq.removeLast();
        }
        evictExpired();
    }

    public Optional<SnapshotDTO> getBySnapshotId(String snapshotId) {
        evictExpired();
        return Optional.ofNullable(bySnapshotId.get(snapshotId));
    }

    public Optional<SnapshotDTO> getLatestByTransformationId(Long transformationId) {
        evictExpired();
        return Optional.ofNullable(latestByTransformationId.get(transformationId));
    }

    public List<SnapshotDTO> listByTransformationId(Long transformationId, int limit) {
        evictExpired();
        Deque<SnapshotDTO> dq = historyByTransformationId.get(transformationId);
        if (dq == null)
            return List.of();
        List<SnapshotDTO> out = new ArrayList<>(Math.min(limit, dq.size()));
        int i = 0;
        for (SnapshotDTO s : dq) {
            if (i++ >= limit)
                break;
            out.add(s);
        }
        return out;
    }

    private void evictExpired() {
        long now = Instant.now().getEpochSecond();

        bySnapshotId.values().removeIf(s -> isExpired(s, now));
        latestByTransformationId.values().removeIf(s -> isExpired(s, now));

        historyByTransformationId.values().forEach(dq -> dq.removeIf(s -> isExpired(s, now)));
        historyByTransformationId.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    private boolean isExpired(SnapshotDTO s, long nowEpochSec) {
        return s.getCreatedAt() != null &&
                (nowEpochSec - s.getCreatedAt().getEpochSecond()) > DEFAULT_TTL_SECONDS;
    }
}