// src/main/java/de/unistuttgart/stayinsync/core/configuration/mapping/SourceSystemEndpointMapper.java
package de.unistuttgart.stayinsync.core.configuration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemEndpointDto;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface SourceSystemEndpointMapper {

    SourceSystemEndpointDto toDto(SourceSystemEndpoint entity);

    SourceSystemEndpoint toEntity(SourceSystemEndpointDto dto);
}
