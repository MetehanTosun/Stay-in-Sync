package de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.CreateArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.GetRequestConfigurationDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.RequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.RequestConfigurationMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
uses = { ActionMapper.class })
public interface RequestConfigurationMapper {

    /**
     * Maps a CreateTargetArcDTO to a TargetSystemApiRequestConfiguration entity.
     * This method ONLY maps the simple fields. Complex objects (TargetSystem, Actions)
     * are ignored and must be handled by the service layer.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "targetSystem", ignore = true)
    @Mapping(target = "actions", ignore = true)
    @Mapping(target = "syncSystemEndpoint", ignore = true)
    @Mapping(target = "queryParameterValues", ignore = true)
    @Mapping(target = "apiRequestHeaders", ignore = true)
    TargetSystemApiRequestConfiguration mapToEntity(CreateArcDTO dto);

    /**
     * Maps a TargetSystemApiRequestConfiguration entity to its detailed DTO for GET requests.
     */
    @Mapping(target = "targetSystemName", source = "targetSystem.name")
    @Mapping(target = "arcType", constant = "REST")
    GetRequestConfigurationDTO mapToGetDTO(TargetSystemApiRequestConfiguration entity);

    List<GetRequestConfigurationDTO> mapToGetDTOList(List<TargetSystemApiRequestConfiguration> entities);

    /**
     * Used for full updates of an existing ARC.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "targetSystem", ignore = true)
    @Mapping(target = "actions", ignore = true)
    void mapFullUpdate(TargetSystemApiRequestConfiguration input, @MappingTarget TargetSystemApiRequestConfiguration target);

    /**
     * Maps a TargetSystemApiRequestConfiguration entity to its message DTO.
     * The `alias` is mapped directly, and the list of actions is delegated
     * to the TargetActionMessageMapper.
     * @param entity The source entity.
     * @return The DTO for the message queue.
     */
    RequestConfigurationMessageDTO mapToMessageDTO(TargetSystemApiRequestConfiguration entity);
}
