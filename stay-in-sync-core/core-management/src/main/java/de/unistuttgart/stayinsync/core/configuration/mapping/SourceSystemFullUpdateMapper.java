package de.unistuttgart.stayinsync.core.configuration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.SourceSystem;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface SourceSystemFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(SourceSystem input, @MappingTarget SourceSystem target);
}