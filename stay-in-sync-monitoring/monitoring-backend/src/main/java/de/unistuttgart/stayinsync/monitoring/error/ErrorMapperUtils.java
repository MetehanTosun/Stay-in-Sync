package de.unistuttgart.stayinsync.monitoring.error;

import jakarta.ws.rs.core.Response;

public class ErrorMapperUtils {

    public static Response.Status resolveHttpStatus(ErrorType type) {
        return switch (type) {
            case VALIDATION_ERROR, AUTHENTICATION_ERROR -> Response.Status.BAD_REQUEST;
            case TIMEOUT -> Response.Status.REQUEST_TIMEOUT;
            case NETWORK_ERROR -> Response.Status.SERVICE_UNAVAILABLE;
            case UNKNOWN_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }
}