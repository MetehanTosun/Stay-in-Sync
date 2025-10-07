package de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

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