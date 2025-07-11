package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateRequestConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.GetRequestConfigurationDTO;
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

    CreateRequestConfigurationDTO mapToDTO(SourceSystemApiRequestConfiguration input);

    SourceSystemApiRequestConfigurationMessageDTO mapToMessageDTO(SourceSystemApiRequestConfiguration input);

    SourceSystemApiRequestConfiguration mapToEntity(CreateRequestConfigurationDTO input);
    
    List<GetRequestConfigurationDTO> mapToDTOList(List<SourceSystemApiRequestConfiguration> input);

    @Mapping(target = "sourceSystemName", source = "sourceSystem.name")
    GetRequestConfigurationDTO mapToDTOGet(SourceSystemApiRequestConfiguration input);
    /**
     * Maps a CreateArcDTO to a SourceSystemApiRequestConfiguration entity.
     * This method ONLY maps the simple fields. All complex objects and collections
     * that require database lookups are ignored and will be handled by the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sourceSystem", ignore = true)
    @Mapping(target = "sourceSystemEndpoint", ignore = true)
    @Mapping(target = "syncSystemEndpoint", ignore = true)
    @Mapping(target = "transformations", ignore = true)
    @Mapping(target = "queryParameterValues", ignore = true)
    @Mapping(target = "apiRequestHeaders", ignore = true)
    SourceSystemApiRequestConfiguration mapToEntity(CreateArcDTO input);
}
