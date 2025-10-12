package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetPropertiesDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.AssetProperties;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper-Klasse zur Konvertierung zwischen EDCProperty-Entities und EDCPropertyDto-Objekten.
 */
@Mapper
 public interface AssetPropertiesMapper {

    AssetPropertiesMapper mapper = Mappers.getMapper(AssetPropertiesMapper.class);

    
    AssetPropertiesDto entityToDto(AssetProperties entity);

    AssetProperties dtoToEntity(AssetPropertiesDto dto);
}

