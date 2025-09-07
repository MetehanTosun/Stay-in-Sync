package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;

import java.util.Map;

/**
 * Mapper-Klasse für die Konvertierung zwischen EDCPolicy-Entitäten und DTOs.
 * 
 * Diese Klasse stellt Methoden bereit, um zwischen den Datenbank-Entitäten (EDCPolicy)
 * und den Data Transfer Objects (EDCPolicyDto) zu konvertieren. Die Hauptaufgabe ist die
 * Serialisierung und Deserialisierung der Policy-Struktur zwischen JSON-String und Map-Objekt.
 */
public class EDCPolicyMapper {

    /**
     * Einmaliger ObjectMapper für die JSON-Serialisierung/Deserialisierung.
     * Wird statisch erstellt, um Performance zu optimieren.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Konvertiert ein DTO-Objekt in eine Datenbank-Entität.
     * 
     * @param dto Das DTO-Objekt mit den Policy-Daten
     * @return Eine neue EDCPolicy-Entität mit den Daten aus dem DTO
     * @throws JsonProcessingException Bei Fehlern während der JSON-Serialisierung
     */
    public static EDCPolicy fromDto(EDCPolicyDto dto) throws JsonProcessingException {
        EDCPolicy entity = new EDCPolicy();
        entity.id = dto.getId(); // Dies ist OK, da id in UuidEntity public ist
        entity.policyId = dto.getPolicyId();
        entity.displayName = dto.getDisplayName();
        
        if (dto.getPolicy() != null) {
            // Policy-Struktur als JSON-String serialisieren
            entity.policyJson = objectMapper.writeValueAsString(dto.getPolicy());
        }
        return entity;
    }

    /**
     * Konvertiert eine Datenbank-Entität in ein DTO-Objekt.
     * 
     * @param entity Die EDCPolicy-Entität aus der Datenbank
     * @return Ein neues DTO-Objekt mit den Daten aus der Entität
     * @throws JsonProcessingException Bei Fehlern während der JSON-Deserialisierung
     */
    public static EDCPolicyDto toDto(EDCPolicy entity) throws JsonProcessingException {
        EDCPolicyDto dto = new EDCPolicyDto();
        dto.setId(entity.id); // Dies ist OK, da id in UuidEntity public ist
        dto.setPolicyId(entity.policyId);
        dto.setDisplayName(entity.displayName);
        
        if (entity.policyJson != null) {
            // JSON-String in Policy-Map-Struktur deserialisieren
            @SuppressWarnings("unchecked")
            Map<String, Object> policyMap = objectMapper.readValue(entity.policyJson, Map.class);
            dto.setPolicy(policyMap);
        }
        return dto;
    }
}
