package de.unistuttgart.stayinsync.core.configuration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiQueryParam;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiQueryParamDto;

/**
 * Mapper für SourceSystemApiQueryParam ⇄ ApiQueryParamDto
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiQueryParamMapper {
    ApiQueryParamDto toDto(SourceSystemApiQueryParam entity);
    SourceSystemApiQueryParam toEntity(ApiQueryParamDto dto);
}