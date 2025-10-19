/**
 * Data Transfer Object (DTO) representing a replay execution request.
 *
 * Sent from the UI to the backend when executing a replay. It contains the
 * transformation script name, the JavaScript code to execute, the input data
 * (as arbitrary JSON), and the generated SDK code injected before execution.
 *
 * @property scriptName - Optional logical name of the script (for stack traces/logs).
 * @property javascriptCode - JavaScript source code containing the transform() function.
 * @property sourceData - JSON data provided to the script as the global 'source' object.
 * @property generatedSdkCode - Auto-generated SDK script injected to provide the targets.* API.
 *
 * @author Mohammed-Ammar Hassnou
 */
// Request DTO (what we send to backend)
export interface ReplayExecuteRequestDTO {
  scriptName: string;
  javascriptCode: string;
  sourceData: any; // backend uses JsonNode → 'any' is safe in TS
  generatedSdkCode: string;
}

/**
 * Data Transfer Object (DTO) representing the result of a replay execution.
 *
 * Returned from the backend after executing a transformation script in the
 * sandboxed replay environment. It provides the script's output, captured
 * variable states, and any error information.
 *
 * @property outputData - JSON result returned by transform(); may be null if failed.
 * @property variables - Map of variable names to their captured values after execution.
 * @property errorInfo - Optional string describing an error or stack trace if execution failed.
 *
 * @author Mohammed-Ammar Hassnou
 */
// Response DTO (what we get back from backend)
export interface ReplayExecuteResponseDTO {
  outputData: any; // backend returns arbitrary JSON
  variables: { [key: string]: any }; // Map<String, Object> in Java → record type in TS
  errorInfo: string | null; // errorInfo may be null if no error
}
