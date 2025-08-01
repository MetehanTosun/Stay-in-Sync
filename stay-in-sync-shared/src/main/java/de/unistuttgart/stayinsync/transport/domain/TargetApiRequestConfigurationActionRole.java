package de.unistuttgart.stayinsync.transport.domain;

public enum TargetApiRequestConfigurationActionRole {
    /** Represents the api-call to check the existence of an entity */
    CHECK,

    /** Represents the api-call to create a new entity */
    CREATE,

    /** Represents the api-call to update an existing entity */
    UPDATE,

    /*
    TODO: Evaluate better pattern setup for custom workflows.
    WORKFLOW_STEP_1,
    etc.
     */
}
