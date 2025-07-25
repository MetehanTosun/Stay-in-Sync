package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.error.ErrorType;
import de.unistuttgart.stayinsync.monitoring.error.ServiceException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.EnumMap;
import java.util.Map;

@ApplicationScoped
public class ErrorLogicService {

    private static final Logger LOG = Logger.getLogger(ErrorLogicService.class);

    @Inject
    MeterRegistry registry;

    private Map<ErrorType, Counter> errorCounters;

    @PostConstruct
    void initCounters() {
        errorCounters = new EnumMap<>(ErrorType.class);
        for (ErrorType type : ErrorType.values()) {
            errorCounters.put(type, Counter.builder("app_errors_total")
                    .tag("type", type.name())
                    .description("Total number of application errors by type")
                    .register(registry));
        }
    }

    private void countError(ErrorType errorType) {
        if (errorCounters != null && errorCounters.containsKey(errorType)) {
            errorCounters.get(errorType).increment();
        }
    }

    /**
     * Simulates a general failure triggered via a query param for test purposes.
     * If "triggerFailure=true", a NETWORK_ERROR is thrown.
     */
    public void simulateFailureIfRequested(boolean triggerFailure) {
        LOG.info("simulateFailureIfRequested triggered with: " + triggerFailure);

        if (triggerFailure) {
            LOG.error("Simulated NETWORK_ERROR: Database not accessible");
            countError(ErrorType.NETWORK_ERROR);
            throw new ServiceException("Database not accessible", ErrorType.NETWORK_ERROR);
        }
    }

    /**
     * Simulates a specific error based on the provided ErrorType.
     * Logs and counts each simulated error type.
     */
    public void simulateErrorByType(ErrorType errorType) {
        LOG.info("simulateErrorByType triggered with: " + errorType);
        countError(errorType);

        switch (errorType) {
            case NETWORK_ERROR -> {
                LOG.error("Simulating NETWORK_ERROR");
                throw new ServiceException("Unable to reach database", errorType);
            }
            case AUTHENTICATION_ERROR -> {
                LOG.warn("Simulating AUTHENTICATION_ERROR");
                throw new ServiceException("Invalid credentials", errorType);
            }
            case TIMEOUT -> {
                LOG.error("Simulating TIMEOUT");
                throw new ServiceException("Request timed out", errorType);
            }
            case VALIDATION_ERROR -> {
                LOG.warn("Simulating VALIDATION_ERROR");
                throw new ServiceException("Invalid input data", errorType);
            }
            case UNKNOWN_ERROR -> {
                LOG.error("Simulating UNKNOWN_ERROR");
                throw new ServiceException("Unexpected internal error", errorType);
            }
        }
    }
    //Fallback method used if retries are exhausted.

    public void fallbackErrorHandler(ErrorType errorType) {
        LOG.warnf("Fallback strategy activated for error type: %s", errorType);

        switch (errorType) {
            case NETWORK_ERROR -> {
                LOG.info("Fallback: Saving the failed request for later retry.");
                // TODO: store request locally or in a queue system
            }
            case TIMEOUT -> {
                LOG.info("Fallback: Notifying admin due to timeout.");
                // TODO:  alert system or trigger webhook
            }
            default -> {
                LOG.info("Fallback: No specific fallback strategy defined.");
            }
        }
    }

}
