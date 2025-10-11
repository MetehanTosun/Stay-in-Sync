package de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface TargetRequestHeaderMessageMapper {

    default List<ApiRequestHeaderMessageDTO> map(Set<ApiHeader> headers){
        if (headers == null || headers.isEmpty()){
            return List.of();
        }
        List<ApiRequestHeaderMessageDTO> list = new ArrayList<>();
        for (ApiHeader h : headers){
            if (h.values == null || h.values.isEmpty()){
                continue;
            }
            // Use the first value for now; can be extended to multiple later
            String value = h.values.iterator().next();
            list.add(new ApiRequestHeaderMessageDTO(h.headerName, value));
        }
        return list;
    }
}


