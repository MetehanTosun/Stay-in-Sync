package de.unistuttgart.stayinsync.core.transport.dto;

public record ApiRequestParameterMessageDTO(ParamType type, String paramName, String paramValue) {
}
