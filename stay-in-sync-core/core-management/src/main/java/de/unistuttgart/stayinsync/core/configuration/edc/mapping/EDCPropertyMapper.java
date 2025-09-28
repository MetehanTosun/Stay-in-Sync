package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty;

/**
 * Mapper-Klasse zur Konvertierung zwischen EDCProperty-Entities und EDCPropertyDto-Objekten.
 */
public class EDCPropertyMapper {

    /**
     * Konvertiert ein EDCProperty-Entity in ein EDCPropertyDto.
     * 
     * @param entity Das zu konvertierende Entity
     * @return Das erzeugte DTO oder null, wenn entity null ist
     */
    public static EDCPropertyDto toDto(EDCProperty entity) {
        if (entity == null) return null;
        
        EDCPropertyDto dto = new EDCPropertyDto();
        dto.setId(entity.id);
        
        // Properties als Map setzen
        if (entity.getName() != null) {
            dto.addProperty(EDCPropertyDto.PROP_NAME, entity.getName());
        }
        
        if (entity.getVersion() != null) {
            dto.addProperty(EDCPropertyDto.PROP_VERSION, entity.getVersion());
        }
        
        if (entity.getContentType() != null) {
            dto.addProperty(EDCPropertyDto.PROP_CONTENTTYPE, entity.getContentType());
        }
        
        if (entity.getDescription() != null) {
            dto.addProperty(EDCPropertyDto.PROP_DESCRIPTION, entity.getDescription());
        }
        
        return dto;
    }

    /**
     * Konvertiert ein EDCPropertyDto in ein EDCProperty-Entity.
     * 
     * @param dto Das zu konvertierende DTO
     * @return Das erzeugte oder aktualisierte Entity oder null, wenn dto null ist
     * @throws IllegalArgumentException wenn die im DTO angegebene ID nicht existiert
     */
    public static EDCProperty fromDto(EDCPropertyDto dto) {
        if (dto == null) return null;
        
        var entity = new EDCProperty();
        
        // Falls ID vorhanden ist, vorhandenes Entity laden (für Updates)
        if (dto.getId() != null) {
            entity = EDCProperty.findById(dto.getId());
            if (entity == null) {
                throw new IllegalArgumentException("EDCProperty " + dto.getId() + " nicht gefunden");
            }
        }
        
        // Felder aus dem DTO übernehmen
        entity.setName(dto.getName());
        entity.setVersion(dto.getVersion());
        entity.setContentType(dto.getContentType());
        entity.setDescription(dto.getDescription());
        
        // Sicherstellen, dass wir eine Beschreibung haben (Default setzen, falls nötig)
        if (entity.getDescription() == null || entity.getDescription().trim().isEmpty()) {
            entity.setDescription("Default description");
        }
        
        // Sicherstellen, dass wir einen Namen haben (Default setzen, falls nötig)
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            entity.setName("Default asset");
        }
        
        return entity;
    }
}
