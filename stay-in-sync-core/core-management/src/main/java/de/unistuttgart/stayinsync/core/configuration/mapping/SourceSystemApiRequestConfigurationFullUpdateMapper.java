package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemApiRequestConfigurationDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface SourceSystemApiRequestConfigurationFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(SourceSystemApiRequestConfiguration input, @MappingTarget SourceSystemApiRequestConfiguration target);

    SourceSystemApiRequestConfigurationDTO mapToDTO(SourceSystemApiRequestConfiguration input);

    SourceSystemApiRequestConfigurationMessageDTO mapToMessageDTO(SourceSystemApiRequestConfiguration input);
    
    SourceSystemApiRequestConfiguration mapToEntity(SourceSystemApiRequestConfigurationDTO input);

    List<SourceSystemApiRequestConfigurationDTO> mapToDTOList(List<SourceSystemApiRequestConfiguration> input);

}
