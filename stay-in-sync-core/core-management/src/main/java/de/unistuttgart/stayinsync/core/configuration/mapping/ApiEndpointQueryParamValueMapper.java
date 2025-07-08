package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParamValue;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpoindQueryParamValueDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiEndpointQueryParamValueMapper {
    @Mapping(target = "requestConfiguration", ignore = true)
    @Mapping(target = "queryParam", ignore = true)
    @Mapping(source = "paramValue", target = "selectedValue")
    ApiEndpointQueryParamValue mapToEntity(ApiEndpoindQueryParamValueDTO queryParamConfigurationDTO);

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiEndpointQueryParamValue input, @MappingTarget ApiEndpointQueryParamValue target);

    List<ApiEndpoindQueryParamValueDTO> mapToDTOList(List<ApiEndpointQueryParamValue> apiRequestHeaders);

}
