package de.unistuttgart.stayinsync.core.configuration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestHeader;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiRequestHeaderDto;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiRequestHeaderMapper {
    ApiRequestHeaderDto toDto(SourceSystemApiRequestHeader entity);
    SourceSystemApiRequestHeader toEntity(ApiRequestHeaderDto dto);
}