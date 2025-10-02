package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeaderValue;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiHeaderDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiHeaderValueDTO;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    uses = { ApiHeaderFullUpdateMapper.class})
public interface ApiHeaderValueMapper {

    Set<ApiHeaderDTO> mapToDTOSet(Set<ApiHeaderValue> entities);

    @Mapping(target = ".", source = "apiHeader")
    @Mapping(target = "values", source = "selectedValue", qualifiedByName = "stringToSingletonSet")
    ApiHeaderDTO mapToApiHeaderDTO(ApiHeaderValue entity);

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

    /**
     * Custom mapping method to convert a single String into a Set<String>.
     * This is required because the DTO expects a Set, but the entity has a single value.
     */
    @Named("stringToSingletonSet")
    default Set<String> stringToSingletonSet(String value) {
        if (value == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(value);
    }
}
