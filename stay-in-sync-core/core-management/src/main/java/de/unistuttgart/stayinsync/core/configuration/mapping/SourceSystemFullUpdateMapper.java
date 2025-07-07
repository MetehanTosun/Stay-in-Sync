package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, uses = AuthConfigMapper.class)
public interface SourceSystemFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(SourceSystem input, @MappingTarget SourceSystem target);

    @Mapping(target = "openApiSpec", ignore = true)
    SourceSystemDTO mapToDTO(SourceSystem input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sourceSystemEndpoints", ignore = true)
    @Mapping(target = "sourceSystemApiRequestConfigurations", ignore = true)
    @Mapping(target = "openApiSpec", ignore = true)
    SourceSystem mapToEntity(SourceSystemDTO input);

    @Mapping(target = "openApiSpec", ignore = true)
    List<SourceSystemDTO> mapToDTOList(List<SourceSystem> input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authConfig", source = "authConfig", qualifiedByName = "mapAuthConfigToEntity")
    @Mapping(target = "openApiSpec", ignore = true)
    SourceSystem mapToEntity(CreateSourceSystemDTO sourceSystemDTO);
}