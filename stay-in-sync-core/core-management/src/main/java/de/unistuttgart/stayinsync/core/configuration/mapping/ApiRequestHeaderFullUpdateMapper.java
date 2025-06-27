package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestHeader;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ApiRequestHeaderFullUpdateMapper {

    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(ApiRequestHeader input, @MappingTarget ApiRequestHeader target);

    ApiRequestHeaderMessageDTO mapToMessageDTO(ApiRequestHeader input);


}
