package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeaderValue;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiHeaderValueDTO;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiHeaderValueMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiHeaderValue input, @MappingTarget ApiHeaderValue target);

    @Mapping(source = "apiHeader.headerName", target = "headerName")
    @Mapping(source = "selectedValue", target = "headerValue")
    ApiRequestHeaderMessageDTO mapToMessageDTO(ApiHeaderValue input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "selectedValue", source = "headerValue")
    @Mapping(target = "requestConfiguration", ignore = true)
    @Mapping(target = "apiHeader", ignore = true)
    ApiHeaderValue mapToEntity(ApiHeaderValueDTO apiHeaderValueDTO);

    List<ApiHeaderValueDTO> mapToDTOList(List<ApiHeaderValue> apiRequestHeaders);

    ApiHeaderValueDTO mapToDTO(ApiHeaderValue apiRequestHeader);
}
