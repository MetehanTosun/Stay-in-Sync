package de.unistuttgart.stayinsync.core.transport.domain;

public enum TargetApiRequestConfigurationPatternType {
    /** Represents a single API-Call (GET, POST or PUT respectively) */
    BASIC_API,

    /** Represents a pattern for the upsertion of a single object */
    OBJECT_UPSERT,

    /** Represents a pattern for the upsertion of a list of objects */
    LIST_UPSERT,

    /** TODO: Supports a user defined, sequential api-call action workflow */
    CUSTOM_WORKFLOW,
}
