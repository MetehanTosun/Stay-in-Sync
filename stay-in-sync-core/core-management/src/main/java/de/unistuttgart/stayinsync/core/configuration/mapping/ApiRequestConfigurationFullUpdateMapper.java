package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfiguration;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestConfigurationMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiRequestConfigurationFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiRequestConfiguration input, @MappingTarget ApiRequestConfiguration target);

    ApiRequestConfigurationMessageDTO mapToMessageDTO(ApiRequestConfiguration input);

    List<ApiRequestConfigurationMessageDTO> mapToDTOList(List<ApiRequestConfiguration> input);
}
