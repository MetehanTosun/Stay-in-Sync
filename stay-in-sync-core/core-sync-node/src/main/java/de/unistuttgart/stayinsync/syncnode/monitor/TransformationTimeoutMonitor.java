package de.unistuttgart.stayinsync.syncnode.monitor;

import de.unistuttgart.stayinsync.syncnode.syncjob.DispatcherStateService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * A background monitoring service responsible for detecting and handling timed-out transformations.
 * <p>
 * The purpose of this monitor is to ensure system resilience by preventing transformations
 * from getting stuck in an incomplete state indefinitely. It periodically scans all
 * in-progress transformations and if one has not shown any activity within a configured
 * timeout period, it triggers a reset to clean up its state. This prevents stale data
 * and resource leaks.
 */
@ApplicationScoped
public class TransformationTimeoutMonitor {

    // CONSTANTS
    private static final String MDC_TRANSFORMATION_ID_KEY = "transformationId";

    private final DispatcherStateService dispatcherStateService;
    private final Duration transformationTimeout;

    /**
     * Constructs the monitor with its required dependencies and configuration.
     * Using constructor injection makes dependencies explicit and allows for immutability.
     *
     * @param dispatcherStateService The service managing the state of transformations.
     * @param transformationTimeout  The configured duration after which an inactive transformation is considered timed out.
     */
    public TransformationTimeoutMonitor(
            DispatcherStateService dispatcherStateService,
            @ConfigProperty(name = "stayinsync.transformation.timeout") Duration transformationTimeout) {
        this.dispatcherStateService = dispatcherStateService;
        this.transformationTimeout = transformationTimeout;
    }

    /**
     * Periodically executes to check for any stale, in-progress transformations.
     * <p>
     * This scheduled method is the core of the monitor. It iterates through all registered
     * transformations and identifies those that have received some data but have not yet completed
     * (i.e., are not 'ready'). If the time since their last recorded activity exceeds the
     * configured timeout, a warning is logged and the transformation's state is reset via the
     * {@link DispatcherStateService}.
     */
    @Scheduled(every = "{stayinsync.transformation.monitor.interval}")
    void checkForTimeouts() {
        Log.debug("Running transformation timeout check...");
        final Instant now = Instant.now();

        dispatcherStateService.getTransformationRegistry().forEach((id, state) -> {
            // A transformation is eligible for a timeout check if it has started but not yet finished.
            boolean isInProgress = !state.isReady() && !state.getReceivedArcs().isEmpty();

            if (isInProgress) {
                Instant lastActivity = Instant.ofEpochMilli(state.getLastActivityTimestamp());
                Duration inactivity = Duration.between(lastActivity, now);

                if (inactivity.compareTo(transformationTimeout) > 0) {
                    // Use try-finally to guarantee MDC cleanup, preventing context leakage to other logs.
                    try {
                        MDC.put(MDC_TRANSFORMATION_ID_KEY, id.toString());
                        Log.warnf("Transformation has timed out after %s of inactivity. Last activity was at %s. Resetting state.",
                                inactivity, lastActivity);
                        state.reset();
                    } finally {
                        MDC.remove(MDC_TRANSFORMATION_ID_KEY);
                    }
                }
            }
        });
    }
}
