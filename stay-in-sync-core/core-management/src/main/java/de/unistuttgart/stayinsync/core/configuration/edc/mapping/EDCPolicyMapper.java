package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;

import java.util.Map;

public class EDCPolicyMapper {

    // 🔑 Einmaliger ObjectMapper für Serialisierung/Deserialisierung
private static final ObjectMapper objectMapper = new ObjectMapper();

public static EDCPolicy fromDto(EDCPolicyDto dto) throws JsonProcessingException {
    EDCPolicy entity = new EDCPolicy();
    entity.id = dto.getId();
    entity.policyId = dto.getPolicyId();
    if (dto.getPolicy() != null) {
        // Policy-Struktur egal wie → einfach als JSON-String
        entity.policyJson = objectMapper.writeValueAsString(dto.getPolicy());
    }
    return entity;
}

public static EDCPolicyDto toDto(EDCPolicy entity) throws JsonProcessingException {
    EDCPolicyDto dto = new EDCPolicyDto();
    dto.setId(entity.id);
    dto.setPolicyId(entity.policyId);
    if (entity.policyJson != null) {
        dto.setPolicy(objectMapper.readValue(entity.policyJson, Map.class));
    }
    return dto;
}

}
