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
    
    EDCInstanceMapper mapper = Mappers.getMapper(EDCInstanceMapper.class);
    
    /**
     * Konvertiert eine EDCInstance-Entität in ein DTO.
     * 
     * @param entity Die zu konvertierende Entität
     * @return Das erzeugte DTO oder null, wenn die Entität null ist
     */
    @Mapping(source = "id", target = "id")
    EDCInstanceDto toDto(EDCInstance entity);
    
    /**
     * Konvertiert ein DTO in eine EDCInstance-Entität.
     * Falls ein DTO mit einer ID übergeben wird, wird versucht, die existierende Entität zu finden.
     * 
     * @param dto Das zu konvertierende DTO
     * @return Die erzeugte oder aktualisierte Entität oder null, wenn das DTO null ist
     */
    @Mapping(source = "id", target = "id")
    EDCInstance fromDto(EDCInstanceDto dto);

}