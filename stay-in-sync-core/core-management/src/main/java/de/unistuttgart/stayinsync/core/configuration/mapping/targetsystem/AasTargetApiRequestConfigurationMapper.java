package de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasTargetArcDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.AasTargetArcMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface AasTargetApiRequestConfigurationMapper {

    @Mapping(target = "arcType", constant = "AAS")
    @Mapping(target = "targetSystemId", source = "targetSystem.id")
    @Mapping(target = "targetSystemName", source = "targetSystem.name")
    @Mapping(target = "submodelId", source = "submodel.id")
    @Mapping(target = "submodelIdShort", source = "submodel.submodelIdShort")
    AasTargetArcDTO mapToDto(AasTargetApiRequestConfiguration source);

    @Mapping(target = "baseUrl", source = "targetSystem.apiUrl")
    @Mapping(target = "submodelId", source = "submodel.submodelId")
    AasTargetArcMessageDTO mapToMessageDTO(AasTargetApiRequestConfiguration source);
}
