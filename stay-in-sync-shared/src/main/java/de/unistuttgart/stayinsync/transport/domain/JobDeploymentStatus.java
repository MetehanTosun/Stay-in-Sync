package de.unistuttgart.stayinsync.transport.domain;

/**
 * Supposed to be used to describe current job deployment status
 *
 */
public enum JobDeploymentStatus {

    DEPLOYED,
    DEPLOYING,
    FAILED,
    RECONFIGURING,
    STOPPING,
    STOPPED
}
