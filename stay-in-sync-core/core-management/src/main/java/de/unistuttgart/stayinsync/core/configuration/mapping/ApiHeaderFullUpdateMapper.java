package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfigurationHeader;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiRequestHeaderDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.HeaderRequestConfigurationDTO;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiHeaderFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiRequestConfigurationHeader input, @MappingTarget ApiRequestConfigurationHeader target);

    @Mapping(source = "apiHeader.headerName", target = "headerName")
    @Mapping(source = "selectedValue", target = "headerValue")
    ApiRequestHeaderMessageDTO mapToMessageDTO(ApiRequestConfigurationHeader input);

    @Mapping(target = "syncSystem", ignore = true)
    ApiHeader mapToEntity(ApiRequestHeaderDTO apiRequestHeaderDTO);

    List<ApiRequestHeaderDTO> mapToDTOList(List<ApiHeader> apiRequestHeaders);

    ApiRequestHeaderDTO mapToDTO(ApiHeader apiRequestHeader);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "selectedValue", source = "headerValue")
    @Mapping(target = "requestConfiguration", ignore = true)
    @Mapping(target = "apiHeader", ignore = true)
    ApiRequestConfigurationHeader mapToEntity(HeaderRequestConfigurationDTO headerRequestConfigurationDTO);

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiHeader apiEndpointQueryParam, @MappingTarget ApiHeader targetSouceSystemEndpoint);
}
