package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TransformationScript;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface TransformationScriptMapper {

    TransformationScriptDTO mapToDTO(TransformationScript script);

    TransformationScript mapToEntity(TransformationScriptDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transformation", ignore = true)
    void mapFullUpdate(TransformationScript input, @MappingTarget TransformationScript target);
}
