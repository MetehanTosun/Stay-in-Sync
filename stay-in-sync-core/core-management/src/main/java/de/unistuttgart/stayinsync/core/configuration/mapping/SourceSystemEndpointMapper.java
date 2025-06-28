package de.unistuttgart.stayinsync.core.configuration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemEndpointDto;

@Mapper(
componentModel = MappingConstants.ComponentModel.JAKARTA,
uses = {
ApiRequestHeaderMapper.class,
ApiQueryParamMapper.class,
SourceSystemVariableMapper.class
}
)
public interface SourceSystemEndpointMapper {


/**
 * Konvertiert ein Entity in ein DTO, inklusive JSON-Schema und Schema-Modus.
 */
@Mapping(target = "id", source = "id")
@Mapping(target = "endpointPath", source = "endpointPath")
@Mapping(target = "httpRequestType", source = "httpRequestType")
@Mapping(target = "pollingActive", source = "pollingActive")
@Mapping(target = "apiQueryParams", source = "apiQueryParams")
@Mapping(target = "apiRequestHeaders", source = "apiRequestHeaders")
@Mapping(target = "sourceSystemVariables", source = "sourceSystemVariable")
@Mapping(target = "jsonSchema", source = "jsonSchema")
@Mapping(target = "schemaMode", source = "schemaMode")
@Mapping(target = "pollingRateInMs", source = "pollingRateInMs")
SourceSystemEndpointDto toDto(SourceSystemEndpoint source);

/**
 * Konvertiert ein DTO in ein Entity. Id und Verknüpfungen zu anderen Entities müssen in der Service-Schicht
 * bzw. in einem Full-Update-Mapper ergänzt werden.
 */
@Mapping(target = "id", ignore = true)
@Mapping(target = "sourceSystem", ignore = true)
@Mapping(target = "transformations", ignore = true)
SourceSystemEndpoint toEntity(SourceSystemEndpointDto dto);


}

