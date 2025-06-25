package de.unistuttgart.stayinsync.core.configuration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemVariable;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemVariableDto;

/**
 * Mapper für SourceSystemVariable ⇄ SourceSystemVariableDto
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface SourceSystemVariableMapper {
    SourceSystemVariableDto toDto(SourceSystemVariable entity);
    SourceSystemVariable toEntity(SourceSystemVariableDto dto);
}