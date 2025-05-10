package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.error.ErrorType;
import de.unistuttgart.stayinsync.monitoring.error.ServiceException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ErrorLogicService {

    /**
     * Simulates a network/database error to test error handling.
     * If “fail=true” is passed, an error is intentionally thrown.
     */
    public void simulateFailureIfRequested(boolean triggerFailure) {
        if (triggerFailure) {
            throw new ServiceException("Database not accessible", ErrorType.NETWORK_ERROR);
        }
    }
    public void simulateErrorByType(ErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR -> throw new ServiceException("Unable to reach database", errorType);
            case AUTHENTICATION_ERROR -> throw new ServiceException("Invalid credentials", errorType);
            case TIMEOUT -> throw new ServiceException("Request timed out", errorType);
            case VALIDATION_ERROR -> throw new ServiceException("Invalid input data", errorType);
            case UNKNOWN_ERROR -> throw new ServiceException("Unexpected internal error", errorType);
        }
    }
}