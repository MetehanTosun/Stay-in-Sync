
/**
 * Mirrors the backend ApiHeaderValueDTO record.
 */
export interface ApiHeaderValueDTO {
    /** The value for the HTTP header */
    headerValue: string;
  
    /** The name of the HTTP header (e.g. "Authorization") */
    headerName: string;
  }