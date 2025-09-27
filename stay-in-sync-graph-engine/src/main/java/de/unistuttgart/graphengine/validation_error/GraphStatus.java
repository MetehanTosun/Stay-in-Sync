package de.unistuttgart.graphengine.validation_error;

/**
 * Represents the validation status of a logic graph.
 */
public enum GraphStatus {
    /**
     * The graph is a draft. It has been saved but has not passed validation.
     * It cannot be executed.
     */
    DRAFT,

    /**
     * The graph has been successfully validated and is considered complete and
     * ready for execution.
     */
    FINALIZED
}
