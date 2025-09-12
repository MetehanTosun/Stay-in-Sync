package de.unistuttgart.stayinsync.core.monitoring.error;

public enum ErrorType {
    // Technical errors
    DATABASE_ERROR,
    NETWORK_ERROR,
    TIMEOUT,
    IO_ERROR,
    SERVICE_UNAVAILABLE,

    // Authentication/authorization error
    AUTHENTICATION_ERROR,
    AUTHORIZATION_ERROR,

    // Application error
    VALIDATION_ERROR,
    BUSINESS_LOGIC_ERROR,
    RESOURCE_NOT_FOUND,
    DUPLICATE_REQUEST,
    CONFLICT,

    // External API/Service error
    EXTERNAL_SERVICE_ERROR,
    THIRD_PARTY_TIMEOUT,

    // Other
    UNKNOWN_ERROR,
    INTERNAL_SERVER_ERROR
}
