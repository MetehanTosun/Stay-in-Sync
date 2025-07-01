package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiEndpointQueryParam;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiEndpointQueryParamDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiEndpointQueryParamMapper {


    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiEndpointQueryParam input, @MappingTarget ApiEndpointQueryParam target);

    @Mapping(ignore = true, target = "syncSystemEndpoint")
    ApiEndpointQueryParam mapToEntity(ApiEndpointQueryParamDTO inputDTO);

    List<ApiEndpointQueryParamDTO> mapToDTOList(List<ApiEndpointQueryParam> allQueryParamsByEndpointId);

    ApiEndpointQueryParamDTO mapToDTO(ApiEndpointQueryParam sourceSystemEndpoint);


}
