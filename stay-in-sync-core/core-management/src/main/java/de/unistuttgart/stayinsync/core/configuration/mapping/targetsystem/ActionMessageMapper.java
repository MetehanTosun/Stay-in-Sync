package de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemApiRequestConfigurationAction;
import de.unistuttgart.stayinsync.core.transport.dto.targetsystems.ActionMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ActionMessageMapper {

    /**
     * Maps a TargetArcAction entity to its flat message DTO representation.
     * It accesses the linked endpoint to flatten the structure.
     * @param action The source entity.
     * @return The flattened DTO for the message queue.
     */
    @Mapping(source = "endpoint.httpRequestType", target = "httpMethod")
    @Mapping(source = "endpoint.endpointPath", target = "path")
    ActionMessageDTO mapToMessageDTO(TargetSystemApiRequestConfigurationAction action);
}
