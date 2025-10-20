package de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Data Transfer Object (DTO) representing a replay execution request.
 * <p>
 * This object is sent to the replay REST endpoint to trigger execution of a
 * transformation script
 * in the sandboxed GraalJS environment. It contains the script metadata, its
 * JavaScript source code,
 * and the input data structure used during execution.
 * </p>
 *
 * <p>
 * Typical usage flow:
 * </p>
 * <ul>
 * <li>The frontend or management component sends this DTO to the
 * <code>/api/replay/execute</code> endpoint.</li>
 * <li>The replay subsystem deserializes it and passes it to
 * {@link de.unistuttgart.stayinsync.monitoring.core.configuration.service.ReplayExecutor}.</li>
 * </ul>
 *
 * @param scriptName       Optional logical name of the script; used for
 *                         identification and stack traces.
 * @param javascriptCode   The JavaScript source code containing the
 *                         <code>transform()</code> function to execute.
 * @param sourceData       JSON input data provided to the transformation script
 *                         as <code>source</code>.
 * @param generatedSdkCode The sdk code for the context of the graalJs instance
 *
 * @author Mohammed-Ammar Hassnou
 */
public record ReplayExecuteRequestDTO(
        String scriptName,
        String javascriptCode,
        JsonNode sourceData,
        String generatedSdkCode) {
}