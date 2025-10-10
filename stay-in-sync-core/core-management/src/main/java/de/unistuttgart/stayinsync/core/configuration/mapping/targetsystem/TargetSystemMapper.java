package de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface TargetSystemMapper {

    @Mapping(target = "openApiSpec", source = "openAPI")
    TargetSystem toEntity(TargetSystemDTO dto);

    @Mapping(target = "openAPI", source = "openApiSpec")
    TargetSystemDTO toDto(TargetSystem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "openApiSpec", source = "openAPI")
    void updateFromDto(TargetSystemDTO dto, @MappingTarget TargetSystem entity);
}