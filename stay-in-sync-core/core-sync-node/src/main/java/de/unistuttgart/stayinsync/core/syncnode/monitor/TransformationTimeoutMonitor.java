package de.unistuttgart.stayinsync.core.syncnode.monitor;

import de.unistuttgart.stayinsync.core.syncnode.syncjob.DispatcherStateService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class TransformationTimeoutMonitor {

    private static final long TIMEOUT_MS = 30_000;

    @Inject
    DispatcherStateService dispatcherStateService;

    @Scheduled(every = "10s")
    void checkForTimeouts() {
        Log.debug("Running transformation timeout check...");
        long now = Instant.now().toEpochMilli();

        Map<Long, DispatcherStateService.TransformationState> registry = dispatcherStateService.getTransformationRegistry();

        for (Map.Entry<Long, DispatcherStateService.TransformationState> entry : registry.entrySet()) {
            DispatcherStateService.TransformationState state = entry.getValue();

            if (!state.isReady() && !state.getReceivedArcs().isEmpty()) {
                long lastActivity = state.getLastActivityTimestamp();
                if (now - lastActivity > TIMEOUT_MS) {
                    MDC.put("transformationId", entry.getKey().toString());
                    Log.warnf("Transformation ID %d has timed out. Last activity was at %s",
                            entry.getKey(), Instant.ofEpochMilli(lastActivity).toString());
                    state.reset();
                }
            }
        }
    }
}
