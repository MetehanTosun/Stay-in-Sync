package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateArcDTO (
        @NotNull String name,

        @NotNull
        Long sourceSystemId,

        @NotNull
        Long endpointId,

        Map<String, String> pathParameterValues,
        Map<String, String> queryParameterValues,
        Map<String, String> headerValues,

        @NotBlank
        String responseDts,

        int pollingIntervallTimeInMs,
        boolean active
) {
}
