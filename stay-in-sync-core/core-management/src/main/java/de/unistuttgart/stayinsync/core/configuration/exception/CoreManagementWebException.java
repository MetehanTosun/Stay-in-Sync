package de.unistuttgart.stayinsync.core.configuration.exception;

import jakarta.ws.rs.core.Response;

/**
 * Compatibility wrapper to preserve legacy tests and callers.
 * Delegates to CoreManagementException with identical semantics.
 */
public class CoreManagementWebException extends CoreManagementException {

    public CoreManagementWebException(Response.Status status, String title, String message) {
        super(status, title, message);
    }

    public CoreManagementWebException(Response.Status status, String title, String message, Object... args) {
        super(status, title, message, args);
    }
}


