package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "DiscoveredEndpoint", description = "A single endpoint discovered from an OpenAPI spec")
public record DiscoveredEndpoint(
  @Schema(description = "Path of the endpoint", example = "/pets")
  String path,
  @Schema(description = "HTTP method of the endpoint", example = "GET")
  String method
) {}