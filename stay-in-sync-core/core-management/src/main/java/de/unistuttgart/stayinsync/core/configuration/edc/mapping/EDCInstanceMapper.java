package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper für die Konvertierung zwischen EDCInstance-Entitäten und DTOs.
 */
@Mapper
public interface EDCInstanceMapper {
    
    EDCInstanceMapper mapper = Mappers.getMapper(EDCInstanceMapper.class);
    
    /**
     * Konvertiert eine EDCInstance-Entität in ein DTO.
     * 
     * @param entity Die zu konvertierende Entität
     * @return Das erzeugte DTO oder null, wenn die Entität null ist
     */
    @Mapping(source = "id", target = "id")
    EDCInstanceDto entityToDto(EDCInstance entity);
    
    /**
     * Konvertiert ein DTO in eine EDCInstance-Entität.
     * Falls ein DTO mit einer ID übergeben wird, wird versucht, die existierende Entität zu finden.
     * 
     * @param dto Das zu konvertierende DTO
     * @return Die erzeugte oder aktualisierte Entität oder null, wenn das DTO null ist
     */
    @Mapping(source = "id", target = "id")
    EDCInstance dtoToEntity(EDCInstanceDto dto);
}