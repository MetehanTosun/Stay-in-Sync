package de.unistuttgart.stayinsync.core.configuration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TargetSystemDTO;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface TargetSystemMapper {

    TargetSystem toEntity(TargetSystemDTO dto);

    @Mapping(target = "id", source = "id")
    TargetSystemDTO toDto(TargetSystem entity);

    @Mapping(target = "id", ignore = true)
    void updateFromDto(TargetSystemDTO dto, @MappingTarget TargetSystem entity);
}