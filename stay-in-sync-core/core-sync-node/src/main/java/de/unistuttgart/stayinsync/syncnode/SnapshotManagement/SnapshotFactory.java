package de.unistuttgart.stayinsync.syncnode.SnapshotManagement;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.mapper.TransformationResultMapper;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;

/**
 * Factory for creating
 * {@link de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO}
 * instances.
 * <p>
 * This class is part of the Snapshot Management and Replay subsystem. It
 * encapsulates
 * the creation of snapshot representations that are later used to reproduce and
 * debug transformation states ("replay"). A snapshot is created when a
 * transformation
 * (executed by the script engine) reaches a point of interest, such as an
 * error, so
 * that the system can persist the state and allow users to replay the execution
 * for
 * diagnosis.
 * </p>
 *
 * <p>
 * The factory centralizes details such as ID generation and timestamping to
 * ensure
 * consistent snapshot metadata across the codebase.
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
public final class SnapshotFactory {

    /**
     * Builds a {@link SnapshotDTO} from a given {@link TransformationResult}.
     * <p>
     * This method performs the minimal, side-effect-free construction needed for a
     * snapshot DTO:
     * <ul>
     * <li>Generates a unique snapshot identifier.</li>
     * <li>Stamps the snapshot with the current creation time.</li>
     * <li>Converts the provided transformation result into its DTO form using
     * {@link de.unistuttgart.stayinsync.syncnode.mapper.TransformationResultMapper}.</li>
     * </ul>
     * The resulting DTO can be persisted and later used for replay to reproduce the
     * transformation context, particularly when an error has occurred.
     * </p>
     *
     * @param tr the transformation result produced by the script engine; represents
     *           the
     *           state to be captured in the snapshot (must not be {@code null})
     * @param om the Jackson {@link ObjectMapper} used by the mapper during
     *           serialization
     *           of complex fields (must not be {@code null})
     * @return a fully-initialized {@link SnapshotDTO} containing metadata and the
     *         mapped
     *         transformation result
     */
    public static SnapshotDTO fromTransformationResult(TransformationResult tr, ObjectMapper om) {
        SnapshotDTO dto = new SnapshotDTO();
        // Assign a unique identifier to correlate and retrieve this snapshot later.
        dto.setSnapshotId(UUID.randomUUID().toString());
        // Record the creation time to order and audit snapshots during
        // replay/debugging.
        dto.setCreatedAt(Instant.now());
        // Persist a transport-friendly representation of the transformation result.
        dto.setTransformationResult(TransformationResultMapper.toDTO(tr, om));
        return dto;
    }
}