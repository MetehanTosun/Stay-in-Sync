package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.service.SnapshotService;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.TransformationResultDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class SnapshotResourceTest {

    @InjectMock
    SnapshotService snapshotService;

    @Test
    void getLatestSnapshot_shouldReturnSnapshot() {
        // Arrange
        SnapshotDTO dto = new SnapshotDTO();
        dto.setSnapshotId("snap-1");
        dto.setCreatedAt(Instant.parse("2024-10-01T12:00:00Z"));
        TransformationResultDTO result = new TransformationResultDTO();
        result.setValidExecution(true);
        dto.setTransformationResult(result);

        when(snapshotService.getLatestSnapshot(42L)).thenReturn(dto);

        // Act + Assert
        given()
                .queryParam("transformationId", 42)
                .when()
                .get("/api/snapshots/latest")
                .then()
                .statusCode(200)
                .body("snapshotId", equalTo("snap-1"))
                .body("createdAt", notNullValue())
                .body("transformationResult.validExecution", equalTo(true));

        verify(snapshotService).getLatestSnapshot(42L);
    }

    @Test
    void getLastFiveSnapshots_shouldReturnList() {
        // Arrange
        SnapshotDTO s1 = new SnapshotDTO();
        s1.setSnapshotId("snap-1");
        s1.setCreatedAt(Instant.now());

        SnapshotDTO s2 = new SnapshotDTO();
        s2.setSnapshotId("snap-2");
        s2.setCreatedAt(Instant.now());

        when(snapshotService.getLastFiveSnapshots(99L)).thenReturn(List.of(s1, s2));

        // Act + Assert
        given()
                .queryParam("transformationId", 99)
                .when()
                .get("/api/snapshots/list")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].snapshotId", equalTo("snap-1"))
                .body("[1].snapshotId", equalTo("snap-2"));

        verify(snapshotService).getLastFiveSnapshots(99L);
    }

    @Test
    void getById_shouldReturnSnapshotWhenFound() {
        // Arrange
        SnapshotDTO dto = new SnapshotDTO();
        dto.setSnapshotId("snap-10");
        dto.setCreatedAt(Instant.now());

        when(snapshotService.getById(10L)).thenReturn(dto);

        // Act + Assert
        given()
                .when()
                .get("/api/snapshots/10")
                .then()
                .statusCode(200)
                .body("snapshotId", equalTo("snap-10"))
                .body("createdAt", notNullValue());

        verify(snapshotService).getById(10L);
    }
}
