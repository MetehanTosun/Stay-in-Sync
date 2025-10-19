package de.unistuttgart.stayinsync.core.syncnode.SnapshotManagement;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.SnapshotManagement.SnapshotFactory;
import de.unistuttgart.stayinsync.syncnode.mapper.TransformationResultMapper;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.TransformationResultDTO;

/**
 * Unit tests for {@link SnapshotFactory}.
 * <p>
 * Verifies that the factory creates a {@link SnapshotDTO} with a unique id,
 * a reasonable creation timestamp, and a mapped transformation result.
 * </p>
 *
 * <p>
 * Note: We avoid calling a non-existent default constructor of
 * {@link TransformationResult} by mocking it and stubbing the static
 * {@link TransformationResultMapper#toDTO(TransformationResult, ObjectMapper)}
 * to return a minimal DTO, decoupling the test from mapper internals.
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
public class SnapshotFactoryTest {

    @Test
    @DisplayName("fromTransformationResult(): sets id, timestamp, and maps result")
    void fromTransformationResult_setsFields() {
        // arrange
        TransformationResult tr = Mockito.mock(TransformationResult.class);
        ObjectMapper om = new ObjectMapper();
        Instant before = Instant.now();

        // act (stub the static mapper to avoid depending on TransformationResult
        // ctor/shape)
        TransformationResultDTO mapped = new TransformationResultDTO();
        try (MockedStatic<TransformationResultMapper> ms = Mockito.mockStatic(TransformationResultMapper.class)) {
            ms.when(() -> TransformationResultMapper.toDTO(tr, om)).thenReturn(mapped);
            SnapshotDTO dto = SnapshotFactory.fromTransformationResult(tr, om);

            // assert: snapshot id is non-null/non-empty
            assertNotNull(dto.getSnapshotId(), "snapshotId should be generated");
            assertFalse(dto.getSnapshotId().isBlank(), "snapshotId should not be blank");

            // assert: createdAt is set to ~now
            assertNotNull(dto.getCreatedAt(), "createdAt must be assigned");
            Instant after = Instant.now();
            assertFalse(dto.getCreatedAt().isBefore(before.minusSeconds(1)), "createdAt should be >= test start - 1s");
            assertFalse(dto.getCreatedAt().isAfter(after.plusSeconds(1)), "createdAt should be <= test end + 1s");

            // assert: transformation result mapping exists and is exactly the stubbed DTO
            assertSame(mapped, dto.getTransformationResult(), "transformationResult should be the mapped DTO");
        }
    }

    @Test
    @DisplayName("fromTransformationResult(): generates unique ids per call")
    void fromTransformationResult_uniqueIds() {
        // arrange
        TransformationResult tr = Mockito.mock(TransformationResult.class);
        ObjectMapper om = new ObjectMapper();

        TransformationResultDTO mapped = new TransformationResultDTO();
        try (MockedStatic<TransformationResultMapper> ms = Mockito.mockStatic(TransformationResultMapper.class)) {
            ms.when(() -> TransformationResultMapper.toDTO(tr, om)).thenReturn(mapped);

            // act
            SnapshotDTO a = SnapshotFactory.fromTransformationResult(tr, om);
            SnapshotDTO b = SnapshotFactory.fromTransformationResult(tr, om);

            // assert
            assertNotEquals(a.getSnapshotId(), b.getSnapshotId(), "Each snapshot should have a unique id");
        }
    }
}
