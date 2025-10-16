package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncNodeClient;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@QuarkusTest
class SnapshotServiceTest {

    @InjectMock
    SyncNodeClient syncNodeClient;

    @Inject
    SnapshotService snapshotService;

    @Test
    void getLatestSnapshot_shouldReturnSnapshot() {
        // Arrange
        SnapshotDTO snapshot = new SnapshotDTO();
        snapshot.setSnapshotId(String.valueOf(1L));
        when(syncNodeClient.getLatest(42L)).thenReturn(snapshot);

        // Act
        SnapshotDTO result = snapshotService.getLatestSnapshot(42L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSnapshotId()).isEqualTo(String.valueOf(1L));
        verify(syncNodeClient).getLatest(42L);
    }

    @Test
    void getLatestSnapshot_shouldReturnNullOnError() {
        // Arrange
        when(syncNodeClient.getLatest(42L)).thenThrow(new RuntimeException("boom"));

        // Act
        SnapshotDTO result = snapshotService.getLatestSnapshot(42L);

        // Assert
        assertThat(result).isNull();
        verify(syncNodeClient).getLatest(42L);
    }

    @Test
    void getLastFiveSnapshots_shouldReturnSnapshots() {
        // Arrange
        SnapshotDTO s1 = new SnapshotDTO();
        s1.setSnapshotId(String.valueOf(10L));
        SnapshotDTO s2 = new SnapshotDTO();
        s2.setSnapshotId(String.valueOf(11L));
        when(syncNodeClient.getLastFive(99L)).thenReturn(List.of(s1, s2));

        // Act
        List<SnapshotDTO> result = snapshotService.getLastFiveSnapshots(99L);

        // Assert
        assertThat(result).hasSize(2).extracting("SnapshotId").containsExactly(String.valueOf(10L), String.valueOf(11L));
        verify(syncNodeClient).getLastFive(99L);
    }

    @Test
    void getLastFiveSnapshots_shouldReturnEmptyOnError() {
        // Arrange
        when(syncNodeClient.getLastFive(99L)).thenThrow(new RuntimeException("fail"));

        // Act
        List<SnapshotDTO> result = snapshotService.getLastFiveSnapshots(99L);

        // Assert
        assertThat(result).isEmpty();
        verify(syncNodeClient).getLastFive(99L);
    }

    @Test
    void getById_shouldReturnSnapshot() {
        // Arrange
        SnapshotDTO snapshot = new SnapshotDTO();
        snapshot.setSnapshotId(String.valueOf("123"));
        when(syncNodeClient.getById(Long.valueOf("123"))).thenReturn(snapshot);

        // Act
        SnapshotDTO result = snapshotService.getById("123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSnapshotId()).isEqualTo(String.valueOf(123L));
        verify(syncNodeClient).getById(Long.valueOf("123"));
    }

    @Test
    void getById_shouldReturnNullOnError() {
        // Arrange
        when(syncNodeClient.getById(Long.valueOf("123"))).thenThrow(new RuntimeException("not found"));

        // Act
        SnapshotDTO result = snapshotService.getById("123");

        // Assert
        assertThat(result).isNull();
        verify(syncNodeClient).getById(Long.valueOf("123"));
    }
}

