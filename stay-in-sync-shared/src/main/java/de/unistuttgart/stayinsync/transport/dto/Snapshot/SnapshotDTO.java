package de.unistuttgart.stayinsync.transport.dto.Snapshot;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotDTO {
    private String snapshotId;
    private Instant createdAt;
    private TransformationResultDTO transformationResult;

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public TransformationResultDTO getTransformationResult() {
        return transformationResult;
    }

    public void setTransformationResult(TransformationResultDTO transformationResult) {
        this.transformationResult = transformationResult;
    }
}