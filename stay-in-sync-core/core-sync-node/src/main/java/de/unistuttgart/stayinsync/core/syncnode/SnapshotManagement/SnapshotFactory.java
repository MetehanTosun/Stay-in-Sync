package de.unistuttgart.stayinsync.core.syncnode.SnapshotManagement;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.core.syncnode.mapper.TransformationResultMapper;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;

public final class SnapshotFactory {

    public static SnapshotDTO fromTransformationResult(TransformationResult tr, ObjectMapper om) {
        SnapshotDTO dto = new SnapshotDTO();
        dto.setSnapshotId(UUID.randomUUID().toString());
        dto.setCreatedAt(Instant.now());
        dto.setTransformationResult(TransformationResultMapper.toDTO(tr, om));
        return dto;
    }
}