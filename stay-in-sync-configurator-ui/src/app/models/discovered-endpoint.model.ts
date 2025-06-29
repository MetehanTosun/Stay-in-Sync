/**
 * DTO for endpoints discovered from an OpenAPI specification.
 */
export interface DiscoveredEndpointDto {
  /** The path of the endpoint, e.g. "/users/{id}" */
  path: string;
  /** The HTTP method for the endpoint, e.g. "GET", "POST" */
  method: string;
}