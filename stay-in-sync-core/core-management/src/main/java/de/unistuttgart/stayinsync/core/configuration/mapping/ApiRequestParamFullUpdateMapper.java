package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestQueryParam;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemEndpointDTO;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiRequestParamFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiRequestQueryParam input, @MappingTarget ApiRequestQueryParam target);

    ApiRequestParameterMessageDTO mapToMessageDTO(ApiRequestConfiguration input);

    List<SourceSystemEndpointDTO> mapToDTOList(List<SourceSystemEndpoint> input);

    SourceSystemEndpoint mapToEntity(SourceSystemEndpointDTO input);

    List<SourceSystemEndpoint> mapToEntityList(List<SourceSystemEndpointDTO> input);
}
