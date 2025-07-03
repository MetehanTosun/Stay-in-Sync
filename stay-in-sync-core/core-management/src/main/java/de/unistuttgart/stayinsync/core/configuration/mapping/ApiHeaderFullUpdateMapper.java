package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiHeaderDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateApiHeaderDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiHeaderFullUpdateMapper {

    @Mapping(target = "syncSystem", ignore = true)
    ApiHeader mapToEntity(ApiHeaderDTO apiRequestHeaderDTO);

    List<ApiHeaderDTO> mapToDTOList(List<ApiHeader> apiRequestHeaders);

    ApiHeaderDTO mapToDTO(ApiHeader apiRequestHeader);


    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiHeader apiEndpointQueryParam, @MappingTarget ApiHeader targetSouceSystemEndpoint);

    ApiHeader mapToEntity(CreateApiHeaderDTO apiEndpointQueryParamDTO);
}
