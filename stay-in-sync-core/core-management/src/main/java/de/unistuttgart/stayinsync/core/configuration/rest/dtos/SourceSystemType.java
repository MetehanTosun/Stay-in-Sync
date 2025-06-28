package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SourceSystemType {
    @JsonProperty("AAS")
    AAS,

    @JsonProperty("REST_OPENAPI")
    REST_OPENAPI
}