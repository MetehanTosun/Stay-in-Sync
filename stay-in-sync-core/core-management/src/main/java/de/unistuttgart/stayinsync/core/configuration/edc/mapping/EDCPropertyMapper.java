package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty;

public class EDCPropertyMapper {

    public static EDCPropertyDto toDto(EDCProperty e) {
        if (e == null) return null;
        return new EDCPropertyDto()
            .setId(e.id)
            .setDescription(e.description);
    }

    public static EDCProperty fromDto(EDCPropertyDto dto) {
        if (dto == null) return null;
        
        var e = new EDCProperty();
        // nur setzen, wenn dto.getId() nicht null: für Updates
        if (dto.getId() != null) {
            e = EDCProperty.findById(dto.getId());
            if (e == null) {
                throw new IllegalArgumentException("EDCProperty " + dto.getId() + " nicht gefunden");
            }
        }
        
        // Beschreibung setzen - verwende explizite description oder versuche aus additionalProperties
        String description = dto.getDescription();
        if (description == null || description.trim().isEmpty()) {
            // Fallback: Versuche description aus additionalProperties zu extrahieren
            if (dto.getAdditionalProperties() != null && dto.getAdditionalProperties().containsKey("asset:prop:description")) {
                description = dto.getAdditionalProperties().get("asset:prop:description");
            }
        }
        
        // Sicherstellen, dass wir eine Beschreibung haben (required field)
        if (description == null || description.trim().isEmpty()) {
            description = "Default description"; // Fallback für required field
        }
        
        e.description = description;
        return e;
    }
}
