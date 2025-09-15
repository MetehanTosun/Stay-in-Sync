package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCDataAddress;

/**
 * Mapper-Klasse zur Konvertierung zwischen EDCDataAddress-Entities und EDCDataAddressDto-Objekten.
 */
public class EDCDataAddressMapper {

    /**
     * Konvertiert ein EDCDataAddress-Entity in ein EDCDataAddressDto.
     * 
     * @param entity Das zu konvertierende Entity
     * @return Das erzeugte DTO oder null, wenn entity null ist
     */
    public static EDCDataAddressDto toDto(EDCDataAddress entity) {
        if (entity == null) {
            return null;
        }
        
        EDCDataAddressDto dto = new EDCDataAddressDto();
        dto.setId(entity.id);
        dto.setJsonLDType(entity.getJsonLDType());
        dto.setType(entity.getType());
        
        // Verwende die konsistente baseUrl-Property
        dto.setBaseUrl(entity.getBaseUrl());
        
        dto.setProxyPath(entity.isProxyPath());
        dto.setProxyQueryParams(entity.isProxyQueryParams());
        return dto;
    }

    /**
     * Konvertiert ein EDCDataAddressDto in ein EDCDataAddress-Entity.
     * 
     * @param dto Das zu konvertierende DTO
     * @return Das erzeugte Entity oder null, wenn dto null ist
     */
    public static EDCDataAddress fromDto(EDCDataAddressDto dto) {
        if (dto == null) {
            return null;
        }
        
        EDCDataAddress entity = new EDCDataAddress();
        entity.setJsonLDType(dto.getJsonLDType());
        entity.setType(dto.getType());
        
        // Verwende die konsistente baseUrl-Property
        entity.setBaseUrl(dto.getBaseUrl());
        
        entity.setProxyPath(dto.getProxyPath());
        entity.setProxyQueryParams(dto.getProxyQueryParams());
        return entity;
    }
}
