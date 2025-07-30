package de.unistuttgart.stayinsync.transport.transformation_rule_shared;

import java.util.List;

/**
 * A record to hold the result of a graph validation process.
 *
 * @param isValid       A boolean indicating if the graph passed all validation checks.
 * @param errorMessages A list of human-readable strings detailing any validation failures.
 */
public record ValidationResult(boolean isValid, List<String> errorMessages) {
}