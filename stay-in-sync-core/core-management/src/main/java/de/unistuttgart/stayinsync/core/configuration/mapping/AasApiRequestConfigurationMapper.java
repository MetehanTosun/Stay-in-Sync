package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSourceApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasArcDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface AasApiRequestConfigurationMapper {
    @Mapping(target = "arcType", constant = "AAS")
    @Mapping(source = "submodel.id", target = "submodelId")
    @Mapping(source = "submodel.submodelIdShort", target = "submodelIdShort")
    @Mapping(source = "sourceSystem.name", target = "sourceSystemName")
    AasArcDTO mapToDto(AasSourceApiRequestConfiguration entity);
}
