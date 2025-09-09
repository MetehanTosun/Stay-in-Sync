package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;

/**
 * Mapper-Klasse für die Konvertierung zwischen EDCPolicy-Entitäten und DTOs.
 * 
 * Diese Klasse stellt Methoden bereit, um zwischen den Datenbank-Entitäten (EDCPolicy)
 * und den Data Transfer Objects (EDCPolicyDto) zu konvertieren. Die Hauptaufgabe ist die
 * Serialisierung und Deserialisierung der Policy-Struktur zwischen JSON-String und Map-Objekt.
 */
public class EDCPolicyMapper {

    private static final Logger LOG = Logger.getLogger(EDCPolicyMapper.class);
    
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
        if (dto == null) {
            LOG.warn("Versuch, ein null EDCPolicyDto zu konvertieren");
            return null;
        }
        
        LOG.debug("Konvertiere EDCPolicyDto zu EDCPolicy: " + dto.getId());
        EDCPolicy entity = new EDCPolicy();
        entity.id = dto.getId(); // Dies ist OK, da id in UuidEntity public ist
        entity.setPolicyId(dto.getPolicyId());
        entity.setDisplayName(dto.getDisplayName());
        
        if (dto.getPolicy() != null) {
            try {
                // Policy-Struktur als JSON-String serialisieren
                entity.setPolicyJson(objectMapper.writeValueAsString(dto.getPolicy()));
                LOG.debug("Policy JSON erfolgreich serialisiert");
            } catch (JsonProcessingException e) {
                LOG.error("Fehler bei der Serialisierung der Policy-Map: " + e.getMessage(), e);
                throw e;
            }
        } else {
            LOG.warn("Policy-Map im DTO ist null, setze policyJson auf null");
        }
        return entity;
    }

    /**
     * Konvertiert eine Datenbank-Entität in ein DTO-Objekt.
     * 
     * @param entity Die EDCPolicy-Entität aus der Datenbank
     * @return Ein Optional mit dem DTO-Objekt oder empty, wenn die Konvertierung fehlschlägt
     */
    public static Optional<EDCPolicyDto> toDto(EDCPolicy entity) {
        if (entity == null) {
            LOG.warn("Versuch, eine null EDCPolicy zu konvertieren");
            return Optional.empty();
        }
        
        LOG.debug("Konvertiere EDCPolicy zu EDCPolicyDto: " + entity.id);
        EDCPolicyDto dto = new EDCPolicyDto();
        dto.setId(entity.id);
        dto.setPolicyId(entity.getPolicyId());
        dto.setDisplayName(entity.getDisplayName());
        
        if (entity.getPolicyJson() != null && !entity.getPolicyJson().isEmpty()) {
            try {
                // JSON-String in Policy-Map-Struktur deserialisieren
                @SuppressWarnings("unchecked")
                Map<String, Object> policyMap = objectMapper.readValue(entity.getPolicyJson(), Map.class);
                dto.setPolicy(policyMap);
                LOG.debug("Policy JSON erfolgreich deserialisiert");
            } catch (JsonProcessingException e) {
                LOG.error("Fehler bei der Deserialisierung des JSON-Strings: " + e.getMessage(), e);
                // Wir geben trotzdem ein DTO zurück, aber ohne Policy-Map
                LOG.warn("Gebe DTO ohne Policy-Map zurück aufgrund eines Deserialisierungsfehlers");
            }
        } else {
            LOG.debug("Policy JSON in der Entity ist null oder leer");
        }
        
        return Optional.of(dto);
    }
}
