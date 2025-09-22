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
class SnapshotPollingServiceTest {

    @InjectMock
    SyncNodeClient syncNodeClient;

    @InjectMock
    @RestClient
    SyncJobClient syncJobClient;

    @InjectMock
    Sse sse;

    @Inject
    SnapshotPollingService service;

    SseEventSink sink;

    @BeforeEach
    void setup() {
        sink = mock(SseEventSink.class);
        when(sink.isClosed()).thenReturn(false);

        // Service einen Fake-Client registrieren
        service.subscribe(sink);

        // SSE EventBuilder mocken
        OutboundSseEvent.Builder builder = mock(OutboundSseEvent.Builder.class, RETURNS_SELF);
        when(sse.newEventBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(mock(OutboundSseEvent.class));
    }

    @Test
    void pollSyncNode_shouldDoNothing_whenNoSnapshots() {
        when(syncNodeClient.getLatestAll()).thenReturn(Map.of());

        service.pollSyncNode();

        // kein Event senden
        verify(sink, never()).send(any());
    }

    @Test
    void pollSyncNode_shouldSendEvents_whenSnapshotChanged() {
        // Arrange SnapshotDTOs
        SnapshotDTO snap1 = new SnapshotDTO();

        MonitoringTransformationDto t1 = new MonitoringTransformationDto();
        t1.id = 100L;
        MonitoringSyncJobDto job = new MonitoringSyncJobDto();
        job.id = 42L;
        job.transformations = List.of(t1);

        when(syncNodeClient.getLatestAll()).thenReturn(Map.of(100L, snap1));
        when(syncJobClient.getAll()).thenReturn(List.of(job));

        // Act
        service.pollSyncNode();

        // Assert → mindestens 2 Events (job-update + transformation-update)
        verify(sink, atLeast(2)).send(any(OutboundSseEvent.class));
    }


    @Test
    void buildTransformationToJobMap_shouldReturnCorrectMapping() {
        MonitoringTransformationDto t1 = new MonitoringTransformationDto();
        t1.id = 1L;
        MonitoringTransformationDto t2 = new MonitoringTransformationDto();
        t2.id = 2L;

        MonitoringSyncJobDto job = new MonitoringSyncJobDto();
        job.id = 99L;
        job.transformations = List.of(t1, t2);

        Map<Long, Long> map = serviceTestHelper_buildMap(List.of(job));

        assert map.get(1L).equals(99L);
        assert map.get(2L).equals(99L);
    }

    // kleiner Trick: private Methode über Reflection aufrufen
    private Map<Long, Long> serviceTestHelper_buildMap(List<MonitoringSyncJobDto> jobs) {
        return serviceTestHelper_buildMapInternal(jobs);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> serviceTestHelper_buildMapInternal(List<MonitoringSyncJobDto> jobs) {
        try {
            var m = SnapshotPollingService.class.getDeclaredMethod("buildTransformationToJobMap", List.class);
            m.setAccessible(true);
            return (Map<Long, Long>) m.invoke(service, jobs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
