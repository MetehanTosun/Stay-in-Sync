package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper für die Konvertierung zwischen EDCInstance-Entitäten und DTOs.
 */
@Mapper(componentModel = "cdi")
public interface EDCInstanceMapper {
    
    EDCInstanceMapper INSTANCE = Mappers.getMapper(EDCInstanceMapper.class);
    
    /**
     * Konvertiert eine EDCInstance-Entität in ein DTO.
     * 
     * @param entity Die zu konvertierende Entität
     * @return Das erzeugte DTO oder null, wenn die Entität null ist
     */
    @Mapping(source = "id", target = "id")
    @Mapping(target = "edcAssetIds", ignore = true) 
    EDCInstanceDto toDto(EDCInstance entity);
    
    /**
     * Konvertiert ein DTO in eine EDCInstance-Entität.
     * Falls ein DTO mit einer ID übergeben wird, wird versucht, die existierende Entität zu finden.
     * 
     * @param dto Das zu konvertierende DTO
     * @return Die erzeugte oder aktualisierte Entität oder null, wenn das DTO null ist
     */
    @Mapping(source = "id", target = "id")
    @Mapping(target = "edcAssets", ignore = true) // Wird nicht automatisch gemappt
    EDCInstance fromDto(EDCInstanceDto dto);
    
    /**
     * Nachbearbeitungsmethode, um die Asset-IDs korrekt zu mappen.
     * Diese Methode wird nach dem automatischen Mapping aufgerufen.
     */
    default EDCInstanceDto postProcessToDto(EDCInstanceDto dto, EDCInstance entity) {
        if (entity == null || dto == null) {
            return dto;
        }
        
        Set<UUID> assetIds = Optional.ofNullable(entity.getEdcAssets())
                .orElse(Set.of())
                .stream()
                .map(asset -> asset.id)
                .collect(Collectors.toUnmodifiableSet());
        dto.setEdcAssetIds(assetIds);
        
        return dto;
    }
    
    /**
     * Findet oder erstellt eine Entität basierend auf der DTO-ID.
     */
    default EDCInstance findOrCreateEntity(EDCInstanceDto dto) {
        if (dto == null) {
            return null;
        }
        
        EDCInstance entity = (dto.getId() != null)
                ? EDCInstance.findById(dto.getId())
                : new EDCInstance();
                
        if (entity == null) {
            entity = new EDCInstance();
        }
        
        return entity;
    }
}
