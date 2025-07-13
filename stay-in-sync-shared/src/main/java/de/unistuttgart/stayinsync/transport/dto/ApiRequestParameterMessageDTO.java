package de.unistuttgart.stayinsync.transport.dto;

public record ApiRequestParameterMessageDTO(ParamType type, String paramName, String paramValue) {
}
