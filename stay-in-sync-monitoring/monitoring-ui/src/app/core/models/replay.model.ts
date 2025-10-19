// Request DTO (what we send to backend)
export interface ReplayExecuteRequestDTO {
  scriptName: string;
  javascriptCode: string;
  sourceData: any; // backend uses JsonNode → 'any' is safe in TS
  generatedSdkCode: string;
}

// Response DTO (what we get back from backend)
export interface ReplayExecuteResponseDTO {
  outputData: any; // backend returns arbitrary JSON
  variables: { [key: string]: any }; // Map<String, Object> in Java → record type in TS
  errorInfo: string | null; // errorInfo may be null if no error
}
