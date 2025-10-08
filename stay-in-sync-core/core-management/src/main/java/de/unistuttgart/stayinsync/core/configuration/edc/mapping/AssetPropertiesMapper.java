package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetPropertiesDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.AssetProperties;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper-Klasse zur Konvertierung zwischen EDCProperty-Entities und EDCPropertyDto-Objekten.
 */
public interface AssetPropertiesMapper {

    AssetPropertiesMapper mapper = Mappers.getMapper(AssetPropertiesMapper.class);

    @Mapping(source = "id", target = "id")
    AssetPropertiesDto entityToDto(AssetProperties entity);

    @Mapping(source = "id", target = "id")
    AssetProperties dtoToEntity(AssetPropertiesDto dto);
}

