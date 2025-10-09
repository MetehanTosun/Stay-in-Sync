package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record PolicyDto(
        @JsonProperty("@type")
        //TODO Im Mapper auf Policy festsetzen
        String type,
        Map<String,Object> contents
){
}
