package de.unistuttgart.stayinsync.transport.dto.Snapshot;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Data Transfer Object (DTO) representing a snapshot of a transformation's
 * state.
 * <p>
 * A snapshot captures the context and result of a transformation execution at a
 * specific point
 * in time, usually when an error occurs during script processing. Snapshots are
 * later used to
 * replay transformations for debugging or analysis purposes.
 * </p>
 *
 * <p>
 * Each snapshot contains:
 * </p>
 * <ul>
 * <li>A unique identifier ({@link #snapshotId})</li>
 * <li>The creation timestamp ({@link #createdAt})</li>
 * <li>The corresponding transformation result
 * ({@link #transformationResult})</li>
 * </ul>
 *
 * @author Mohammed-Ammar Hassnou
 */
public class SnapshotDTO {
    /** Unique identifier of this snapshot. */
    private String snapshotId;
    /** Timestamp indicating when the snapshot was created. */
    private Instant createdAt;
    /** The transformation result data captured in this snapshot. */
    private TransformationResultDTO transformationResult;

    /**
     * Get the unique snapshot identifier.
     *
     * @return the snapshot ID
     */
    public String getSnapshotId() {
        return snapshotId;
    }

    /**
     * Set the unique snapshot identifier.
     *
     * @param snapshotId the snapshot ID to assign
     */
    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    /**
     * Get the timestamp when this snapshot was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the timestamp when this snapshot was created.
     *
     * @param createdAt the creation time to assign
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the transformation result contained in this snapshot.
     *
     * @return the transformation result DTO
     */
    public TransformationResultDTO getTransformationResult() {
        return transformationResult;
    }

    /**
     * Set the transformation result contained in this snapshot.
     *
     * @param transformationResult the transformation result DTO to assign
     */
    public void setTransformationResult(TransformationResultDTO transformationResult) {
        this.transformationResult = transformationResult;
    }
}