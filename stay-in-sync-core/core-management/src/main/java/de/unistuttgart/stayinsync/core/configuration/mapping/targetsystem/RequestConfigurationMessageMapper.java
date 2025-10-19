package de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.RequestConfigurationMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = { ActionMessageMapper.class, TargetRequestHeaderMessageMapper.class })
public interface RequestConfigurationMessageMapper {

    /**
     * Maps a TargetSystemApiRequestConfiguration entity to its message DTO.
     * The `alias` is mapped directly, and the list of actions is delegated
     * to the TargetActionMessageMapper.
     * @param entity The source entity.
     * @return The DTO for the message queue.
     */
    @Mapping(source = "targetSystem.apiUrl", target = "baseUrl")
    @Mapping(source = "targetSystem.apiRequestHeaders", target = "headers")
    RequestConfigurationMessageDTO mapToMessageDTO(TargetSystemApiRequestConfiguration entity);
}
