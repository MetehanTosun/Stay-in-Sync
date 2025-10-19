package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateTargetSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TargetSystemEndpointDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface TargetSystemEndpointFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(TargetSystemEndpoint input, @MappingTarget TargetSystemEndpoint target);

    @Mapping(source = "targetSystem.id", target = "targetSystemId")
    TargetSystemEndpointDTO mapToDTO(TargetSystemEndpoint input);

    List<TargetSystemEndpointDTO> mapToDTOList(List<TargetSystemEndpoint> input);

    @Mapping(target = "id", ignore = true)
    TargetSystemEndpoint mapToEntity(TargetSystemEndpointDTO input);

    List<TargetSystemEndpoint> mapToEntityList(List<TargetSystemEndpointDTO> input);

    TargetSystemEndpoint mapToEntity(CreateTargetSystemEndpointDTO createDto);
}


