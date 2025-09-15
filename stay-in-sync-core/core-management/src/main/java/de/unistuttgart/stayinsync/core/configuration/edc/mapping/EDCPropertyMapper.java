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
        
        return new EDCPropertyDto()
            .setId(entity.id)
            .setName(entity.getName())
            .setVersion(entity.getVersion())
            .setContentType(entity.getContentType())
            .setDescription(entity.getDescription());
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
        
        // Bekannte Properties aus additionalProperties übernehmen, falls direkte Felder nicht gesetzt sind
        if (dto.getAdditionalProperties() != null) {
            // Name
            if (entity.getName() == null && dto.getAdditionalProperties().containsKey("asset:prop:name")) {
                entity.setName(dto.getAdditionalProperties().get("asset:prop:name"));
            }
            
            // Version
            if (entity.getVersion() == null && dto.getAdditionalProperties().containsKey("asset:prop:version")) {
                entity.setVersion(dto.getAdditionalProperties().get("asset:prop:version"));
            }
            
            // Content Type
            if (entity.getContentType() == null && dto.getAdditionalProperties().containsKey("asset:prop:contenttype")) {
                entity.setContentType(dto.getAdditionalProperties().get("asset:prop:contenttype"));
            }
            
            // Beschreibung
            if (entity.getDescription() == null && dto.getAdditionalProperties().containsKey("asset:prop:description")) {
                entity.setDescription(dto.getAdditionalProperties().get("asset:prop:description"));
            }
        }
        
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
