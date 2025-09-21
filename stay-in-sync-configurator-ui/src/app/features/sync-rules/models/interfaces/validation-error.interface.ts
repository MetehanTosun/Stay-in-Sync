/**
 * Represents the structure of validation errors received from the backend
 */
export interface ValidationError {
  errorCode: string,
  message: string,
  nodeId?: number,
  NodeName?: string
}
