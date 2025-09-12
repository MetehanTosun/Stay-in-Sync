package de.unistuttgart.stayinsync.core.monitoring.error;

import jakarta.ws.rs.core.Response;

public class ErrorMapperUtils {

    public static Response.Status resolveHttpStatus(ErrorType type) {
        return switch (type) {
            case VALIDATION_ERROR, AUTHENTICATION_ERROR -> Response.Status.BAD_REQUEST;
            case TIMEOUT -> Response.Status.REQUEST_TIMEOUT;
            case DATABASE_ERROR -> null;
            case NETWORK_ERROR -> Response.Status.SERVICE_UNAVAILABLE;
            case UNKNOWN_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
            case IO_ERROR -> null; // Right responds will be added
            case SERVICE_UNAVAILABLE -> null;
            case AUTHORIZATION_ERROR -> null;
            case BUSINESS_LOGIC_ERROR -> null;
            case RESOURCE_NOT_FOUND -> null;
            case DUPLICATE_REQUEST -> null;
            case CONFLICT -> null;
            case EXTERNAL_SERVICE_ERROR -> null;
            case THIRD_PARTY_TIMEOUT -> null;
            case INTERNAL_SERVER_ERROR -> null;
        };
    }
}