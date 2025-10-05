package de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfigurationAction;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.GetActionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ActionMapper {

    @Mapping(target = "endpointId", source = "endpoint.id")
    @Mapping(target = "endpointPath", source = "endpoint.endpointPath")
    @Mapping(target = "httpMethod", source = "endpoint.httpRequestType")
    GetActionDTO mapToGetDTO(TargetSystemApiRequestConfigurationAction action);
}