package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, uses = AuthConfigMapper.class)
public interface SourceSystemFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(SourceSystem input, @MappingTarget SourceSystem target);

    SourceSystemDTO mapToDTO(SourceSystem input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sourceSystemEndpoints", ignore = true)
    SourceSystem mapToEntity(SourceSystemDTO input);

    List<SourceSystemDTO> mapToDTOList(List<SourceSystem> input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authConfig", source = "authConfig", qualifiedByName = "mapAuthConfigToEntity")
    SourceSystem mapToEntity(CreateSourceSystemDTO sourceSystemDTO);

    @Mapping(target="name", source="name")
    @Mapping(target="apiUrl", source="apiUrl")
    @Mapping(target="apiType", source="apiType")
    @Mapping(target="authDetails", source="authConfig", qualifiedByName = "mapAuthConfigToDTO")
    SourceSystemMessageDTO mapToMessageDTO(SourceSystem sourceSystem);
}