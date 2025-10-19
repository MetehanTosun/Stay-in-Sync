package de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay;

import java.util.Map;

/**
 * Data Transfer Object (DTO) representing the result of a replay execution.
 * <p>
 * This object is returned by the replay REST endpoint after executing a
 * transformation script
 * in the sandboxed GraalJS environment. It encapsulates the execution outcome
 * including the
 * script's return value, all captured variable states, and any error message if
 * the execution failed.
 * </p>
 *
 * @param outputData the value returned by the executed {@code transform()}
 *                   function, if successful; may be {@code null}
 * @param variables  a map of variable names to their captured values at the
 *                   replay breakpoint
 * @param errorInfo  details about an error or exception thrown during replay
 *                   execution; {@code null} when execution succeeded
 *
 * @author Mohammed-Ammar Hassnou
 */
public record ReplayExecuteResponseDTO(

                Object outputData,
                Map<String, Object> variables,
                String errorInfo) {
}
