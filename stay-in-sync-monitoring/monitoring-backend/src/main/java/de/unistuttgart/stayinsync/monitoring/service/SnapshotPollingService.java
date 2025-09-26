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

/**
 * SnapshotPollingService periodically polls the SyncNode service for the latest snapshot states
 * of all transformations. If changes are detected, it sends out Server-Sent Events (SSE) to
 * subscribed clients.
 *
 * Responsibilities:
 * - Maintain a list of SSE subscribers.
 * - Poll SyncNode every 10 seconds for updates.
 * - Compare current snapshot state with the previous one.
 * - Broadcast updates about changed jobs and transformations.
 */
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

    //Active SSE clients subscribed to updates
    private final Set<SseEventSink> clients = new CopyOnWriteArraySet<>();

    /** Stores the last known snapshot state to detect changes. */
    private Map<Long, SnapshotDTO> lastSnapshotState = Map.of();

    /**
     * Endpoint to subscribe to server-sent events (SSE).
     * Clients will receive updates when job or transformation states change.
     */
    @GET
    @Path("/subscribe")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@jakarta.ws.rs.core.Context SseEventSink sink) {
        clients.add(sink);
    }

    /**
     * Polls the SyncNode service every 10 seconds to check for snapshot updates.
     * If changes are detected, broadcasts job and transformation updates to all clients.
     */
    @Scheduled(every = "10s")
    public void pollSyncNode() {
        // Fetch latest snapshot state for all transformations
        Map<Long, SnapshotDTO> current = syncNodeClient.getLatestAll();
        if (current.isEmpty()) {
            return; // Do nothing if SyncNode is not reachable
        }

        // Fetch all SyncJobs to map transformations back to their parent job
        List<MonitoringSyncJobDto> jobs = syncJobClient.getAll();
        Map<Long, Long> transformationToJob = buildTransformationToJobMap(jobs);

        // Track changed jobs and transformations
        Set<Long> changedJobs = new HashSet<>();
        Set<Long> changedTransformations = new HashSet<>();

        for (Map.Entry<Long, SnapshotDTO> entry : current.entrySet()) {
            Long transformationId = entry.getKey();
            SnapshotDTO newSnapshot = entry.getValue();
            SnapshotDTO oldSnapshot = lastSnapshotState.get(transformationId);

            // If snapshot is new or has changed → mark job and transformation as changed
            if (oldSnapshot == null || !oldSnapshot.equals(newSnapshot)) {
                Long jobId = transformationToJob.get(transformationId);
                if (jobId != null) {
                    changedJobs.add(jobId);
                }
                changedTransformations.add(transformationId);
            }
        }

        // Send SSE events only if there were changes
        if (!changedJobs.isEmpty()) {
            // Event with job IDs
            OutboundSseEvent jobEvent = sse.newEventBuilder()
                    .name("job-update")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(changedJobs)
                    .build();

            // Event with transformation IDs
            OutboundSseEvent transformationEvent = sse.newEventBuilder()
                    .name("transformation-update")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(changedTransformations)
                    .build();

            // Broadcast to all connected clients
            for (SseEventSink sink : clients) {
                if (!sink.isClosed()) {
                    sink.send(jobEvent);
                    sink.send(transformationEvent);
                }
            }
        }

        // Update last known state
        lastSnapshotState = current;
    }

    /**
     * Builds a mapping from transformation ID to its parent job ID.
     *
     * @param jobs The list of monitoring jobs including their transformations.
     * @return A map where the key is a transformation ID and the value is its parent job ID.
     */
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


