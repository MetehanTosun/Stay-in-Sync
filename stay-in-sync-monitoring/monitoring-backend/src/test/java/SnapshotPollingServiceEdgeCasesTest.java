import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncJobClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncNodeClient;
import de.unistuttgart.stayinsync.monitoring.service.SnapshotPollingService;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSyncJobDto;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@QuarkusTest
class SnapshotPollingServiceEdgeCasesTest {

    @InjectMock
    SyncNodeClient syncNodeClient;

    @InjectMock
    @RestClient
    SyncJobClient syncJobClient;

    @InjectMock
    Sse sse;

    @Inject
    SnapshotPollingService service;

    SseEventSink sink1;
    SseEventSink sink2;

    @BeforeEach
    void setup() {
        sink1 = mock(SseEventSink.class);
        sink2 = mock(SseEventSink.class);
        when(sink1.isClosed()).thenReturn(false);
        when(sink2.isClosed()).thenReturn(false);

        service.subscribe(sink1);
        service.subscribe(sink2);

        OutboundSseEvent.Builder builder = mock(OutboundSseEvent.Builder.class, RETURNS_SELF);
        when(sse.newEventBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(mock(OutboundSseEvent.class));
    }

    @Test
    void pollSyncNode_shouldNotSendEvents_whenSnapshotsUnchanged() {
        SnapshotDTO snap = new SnapshotDTO();
        MonitoringTransformationDto t1 = new MonitoringTransformationDto();
        t1.id = 101L;
        MonitoringSyncJobDto job = new MonitoringSyncJobDto();
        job.id = 10L;
        job.transformations = List.of(t1);

        when(syncNodeClient.getLatestAll()).thenReturn(Map.of(101L, snap));
        when(syncJobClient.getAll()).thenReturn(List.of(job));

        // First poll → sets internal state
        service.pollSyncNode();
        reset(sink1, sink2);

        // Second poll → same snapshot
        when(syncNodeClient.getLatestAll()).thenReturn(Map.of(101L, snap));
        service.pollSyncNode();

        verify(sink1, never()).send(any());
        verify(sink2, never()).send(any());
    }

    @Test
    void pollSyncNode_shouldSkipClosedSinks() {
        SseEventSink closedSink = mock(SseEventSink.class);
        when(closedSink.isClosed()).thenReturn(true);
        service.subscribe(closedSink);

        SnapshotDTO snap = new SnapshotDTO();
        MonitoringTransformationDto t1 = new MonitoringTransformationDto();
        t1.id = 202L;
        MonitoringSyncJobDto job = new MonitoringSyncJobDto();
        job.id = 20L;
        job.transformations = List.of(t1);

        when(syncNodeClient.getLatestAll()).thenReturn(Map.of(202L, snap));
        when(syncJobClient.getAll()).thenReturn(List.of(job));

        service.pollSyncNode();

        verify(sink1, atLeastOnce()).send(any());
        verify(sink2, atLeastOnce()).send(any());
        verify(closedSink, never()).send(any());
    }

    @Test
    void buildTransformationToJobMap_shouldHandleEmptyAndNullTransformations() {
        // Job with null transformations
        MonitoringSyncJobDto job1 = new MonitoringSyncJobDto();
        job1.id = 1L;
        job1.transformations = null;

        // Job with empty transformations
        MonitoringSyncJobDto job2 = new MonitoringSyncJobDto();
        job2.id = 2L;
        job2.transformations = List.of();

        Map<Long, Long> map = buildMapHelper(List.of(job1, job2));
        assert map.isEmpty();
    }

    @Test
    void pollSyncNode_shouldHandleNewTransformationWithoutJob() {
        SnapshotDTO snap = new SnapshotDTO();
        when(syncNodeClient.getLatestAll()).thenReturn(Map.of(500L, snap));
        when(syncJobClient.getAll()).thenReturn(List.of()); // no jobs

        service.pollSyncNode();

        // Transformation event sent but no job event
        verify(sink1, atLeastOnce()).send(any());
        verify(sink2, atLeastOnce()).send(any());
    }

    // helper to call private buildTransformationToJobMap
    @SuppressWarnings("unchecked")
    private Map<Long, Long> buildMapHelper(List<MonitoringSyncJobDto> jobs) {
        try {
            var method = SnapshotPollingService.class.getDeclaredMethod("buildTransformationToJobMap", List.class);
            method.setAccessible(true);
            return (Map<Long, Long>) method.invoke(service, jobs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
