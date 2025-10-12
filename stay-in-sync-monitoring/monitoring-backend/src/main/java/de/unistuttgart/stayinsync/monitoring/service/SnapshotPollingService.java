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
 * Service that periodically polls the SyncNode for the latest snapshots
 * and pushes update events to connected clients via Server-Sent Events (SSE).
 * <p>
 * This class acts as a bridge between the backend SyncNode/SyncJob services
 * and web clients that subscribe to snapshot updates. Clients receive
 * two types of SSE events:
 * <ul>
 *     <li><b>job-update</b> – contains a set of job IDs that have changed.</li>
 *     <li><b>transformation-update</b> – contains a set of transformation IDs that have changed.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Polling is scheduled every 10 seconds. If SyncNode is not reachable
 * or returns no snapshots, no events are sent.
 * </p>
 */
@Singleton
@Startup
@Path("/events")
public class SnapshotPollingService {

    /** Client to fetch snapshots from SyncNode. */
    @Inject
    SyncNodeClient syncNodeClient;

    /** Client to fetch job information from SyncJob service. */
    @Inject
    @RestClient
    SyncJobClient syncJobClient;

    /** SSE factory used to build events. */
    @Inject
    Sse sse;

    /** Connected SSE clients (thread-safe). */
    private final Set<SseEventSink> clients = new CopyOnWriteArraySet<>();

    /** Cached snapshot state from the last poll, used for change detection. */
    private Map<Long, SnapshotDTO> lastSnapshotState = Map.of();

    /**
     * Endpoint for clients to subscribe to snapshot update events via SSE.
     * <p>
     * Connected clients are added to a set and will receive periodic update events
     * if changes are detected in SyncNode snapshots.
     * </p>
     *
     * @param sink the SSE event sink representing the client connection
     */
    @GET
    @Path("/subscribe")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@jakarta.ws.rs.core.Context SseEventSink sink) {
        clients.add(sink);
    }

    /**
     * Polls the SyncNode every 10 seconds for the latest snapshots.
     * <p>
     * - Compares new snapshots with the previously cached state.<br>
     * - Detects changes per transformation.<br>
     * - Maps transformations to their corresponding job IDs.<br>
     * - Sends SSE events to all subscribed clients if any changes occur.
     * </p>
     */
    @Scheduled(every = "10s")
    public void pollSyncNode() {
        Map<Long, SnapshotDTO> current = syncNodeClient.getLatestAll();
        if (current.isEmpty()) {
            // Do nothing if SyncNode is not reachable or returned no snapshots
            return;
        }

        // Fetch all SyncJobs and build a mapping of transformationId -> jobId
        List<MonitoringSyncJobDto> jobs = syncJobClient.getAll();
        Map<Long, Long> transformationToJob = buildTransformationToJobMap(jobs);

        // Detect changes in snapshots compared to the last poll
        Set<Long> changedJobs = new HashSet<>();
        Set<Long> changedTransformations = new HashSet<>();
        for (Map.Entry<Long, SnapshotDTO> entry : current.entrySet()) {
            Long transformationId = entry.getKey();
            SnapshotDTO newSnapshot = entry.getValue();
            SnapshotDTO oldSnapshot = lastSnapshotState.get(transformationId);

            if (oldSnapshot == null || !oldSnapshot.equals(newSnapshot)) {
                // Mark transformation as changed
                changedTransformations.add(transformationId);

                // Also mark its corresponding job as changed (if available)
                Long jobId = transformationToJob.get(transformationId);
                if (jobId != null) {
                    changedJobs.add(jobId);
                }
            }
        }

        // Only send events if there were actual changes
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

            // Push events to all active SSE clients
            for (SseEventSink sink : clients) {
                if (!sink.isClosed()) {
                    sink.send(jobEvent);
                    sink.send(transformationEvent);
                }
            }
        }

        // Update cached state for next comparison
        lastSnapshotState = current;
    }

    /**
     * Builds a mapping of transformation IDs to job IDs
     * from a list of monitoring jobs.
     *
     * @param jobs list of jobs containing transformations
     * @return a map where keys are transformation IDs and values are job IDs
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
