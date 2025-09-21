package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemEndpointDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface SourceSystemEndpointFullUpdateMapper {

    /**
     * Maps all fields except <code>id</code> from {@code input} onto {@code target}.
     *
     * @param input  The input {@link SourceSystemEndpoint}
     * @param target The target {@link SourceSystemEndpoint}
     */
    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(SourceSystemEndpoint input, @MappingTarget SourceSystemEndpoint target);

    @Mapping(source = "sourceSystem.id", target = "sourceSystemId")
    @Mapping(source = "requestBodySchema", target = "requestBodySchema")
    @Mapping(source = "responseBodySchema", target = "responseBodySchema")
    @Mapping(source = "responseDts", target = "responseDts")
    SourceSystemEndpointDTO mapToDTO(SourceSystemEndpoint input);

    List<SourceSystemEndpointDTO> mapToDTOList(List<SourceSystemEndpoint> input);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "requestBodySchema", target = "requestBodySchema")
    @Mapping(source = "responseBodySchema", target = "responseBodySchema")
    @Mapping(source = "responseDts", target = "responseDts")
    SourceSystemEndpoint mapToEntity(SourceSystemEndpointDTO input);

    List<SourceSystemEndpoint> mapToEntityList(List<SourceSystemEndpointDTO> input);

    @Mapping(source = "requestBodySchema", target = "requestBodySchema")
    @Mapping(source = "responseBodySchema", target = "responseBodySchema")
    SourceSystemEndpoint mapToEntity(CreateSourceSystemEndpointDTO sourceSystemEndpointDTO);
}