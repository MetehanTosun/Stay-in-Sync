package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamValueDTO;
import de.unistuttgart.stayinsync.core.transport.dto.ApiRequestParameterMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiEndpointQueryParamValueMapper {
    @Mapping(target = "requestConfiguration", ignore = true)
    @Mapping(target = "queryParam", ignore = true)
    @Mapping(source = "paramValue", target = "selectedValue")
    ApiEndpointQueryParamValue mapToEntity(ApiEndpointQueryParamValueDTO queryParamConfigurationDTO);

    @Mapping(target = "paramName", source = "queryParam.paramName")
    @Mapping(target = "type", source = "queryParam.queryParamType")
    @Mapping(target = "paramValue", source = "selectedValue")
    ApiRequestParameterMessageDTO mapToRequestParameterDTO(ApiEndpointQueryParamValue queryParamValue);

    Set<ApiRequestParameterMessageDTO> mapToRequestParameterMessageSet(Set<ApiEndpointQueryParamValue> queryParamValue);

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiEndpointQueryParamValue input, @MappingTarget ApiEndpointQueryParamValue target);

    List<ApiEndpointQueryParamValueDTO> mapToDTOList(List<ApiEndpointQueryParamValue> apiRequestHeaders);

}
