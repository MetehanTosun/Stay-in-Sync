package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncJobClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncNodeClient;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSyncJobDto;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
@Startup
@Path("/events")
public class SnapshotPollingService {

    @Inject
    SyncNodeClient syncNodeClient;

    @Inject
    @RestClient
    SyncJobClient syncJobClient;

    @Inject
    Sse sse;

    private final Set<SseEventSink> clients = new CopyOnWriteArraySet<>();
    private Map<Long, SnapshotDTO> lastSnapshotState = Map.of();

    @GET
    @Path("/subscribe")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@jakarta.ws.rs.core.Context SseEventSink sink) {
        clients.add(sink);
    }

//    @Scheduled(every = "10s")
    public void pollSyncNode() {
        Map<Long, SnapshotDTO> current = syncNodeClient.getLatestAll();
        if (current.isEmpty()) {
            return; // nichts tun, wenn SyncNode nicht erreichbar
        }

        // Alle SyncJobs holen
        List<MonitoringSyncJobDto> jobs = syncJobClient.getAll();
        Map<Long, Long> transformationToJob = buildTransformationToJobMap(jobs);

        // Änderungen suchen
        Set<Long> changedJobs = new HashSet<>();
        Set<Long> changedTransformations = new HashSet<>();
        for (Map.Entry<Long, SnapshotDTO> entry : current.entrySet()) {
            Long transformationId = entry.getKey();
            SnapshotDTO newSnapshot = entry.getValue();
            SnapshotDTO oldSnapshot = lastSnapshotState.get(transformationId);

            if (oldSnapshot == null || !oldSnapshot.equals(newSnapshot)) {
                Long jobId = transformationToJob.get(transformationId);
                if (jobId != null) {
                    changedJobs.add(jobId);
                }
                changedTransformations.add(transformationId); // zusätzlich TransformationId speichern
            }
        }

        // Nur senden, wenn es Änderungen gibt
        if (!changedJobs.isEmpty()) {
            // Event mit JobIds
            OutboundSseEvent jobEvent = sse.newEventBuilder()
                    .name("job-update")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(changedJobs)
                    .build();

            // Event mit TransformationIds
            OutboundSseEvent transformationEvent = sse.newEventBuilder()
                    .name("transformation-update")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(changedTransformations)
                    .build();

            for (SseEventSink sink : clients) {
                if (!sink.isClosed()) {
                    sink.send(jobEvent);
                    sink.send(transformationEvent);
                }
            }
        }

        lastSnapshotState = current;
    }


    private Map<Long, Long> buildTransformationToJobMap(List<MonitoringSyncJobDto> jobs) {
        Map<Long, Long> map = new HashMap<>();
        for (MonitoringSyncJobDto job : jobs) {
            if (job.transformations != null) {
                for (MonitoringTransformationDto t : job.transformations) {
                    map.put(t.id, job.id);
                }
            }
        }
        return map;
    }
}

