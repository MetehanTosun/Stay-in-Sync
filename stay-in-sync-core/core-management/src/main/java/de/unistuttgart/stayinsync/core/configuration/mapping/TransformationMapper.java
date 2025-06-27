package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationDetailsDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import org.mapstruct.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, uses = { TransformationScriptMapper.class })
public interface TransformationMapper {

    // TODO: Add TransformationRule mappings
    // TODO: Handle complex object mappings in service layer

    /**
     * Maps a complete Transformation entity to its detailed DTO representation.
     * This is used for all GET endpoints (e.g., get by ID, get all).
     */
    @Mapping(source = "syncJob.id", target = "syncJobId")
    @Mapping(source = "targetSystemEndpoint.id", target = "targetSystemEndpointId")
    @Mapping(source = "transformationRule.id", target = "transformationRuleId")
    @Mapping(source = "transformationScript", target = "script") // Delegates to TransformationScriptMapper
    @Mapping(source = "sourceSystemEndpoints", target = "sourceSystemEndpointIds", qualifiedByName = "mapEndpointsToIds")
    TransformationDetailsDTO mapToDetailsDTO(Transformation transformation);

    /**
     * Updates a new Transformation entity from a TransformationShellDTO.
     * This is used by the 'createShell' service method. It only sets the initial fields
     * and ignores all relationships, which will be assembled later.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "syncJob", ignore = true)
    @Mapping(target = "targetSystemEndpoint", ignore = true)
    @Mapping(target = "transformationRule", ignore = true)
    @Mapping(target = "transformationScript", ignore = true)
    @Mapping(target = "sourceSystemEndpoints", ignore = true)
    @Mapping(target = "sourceSystemVariables", ignore = true)
    void updateFromShellDTO(TransformationShellDTO dto, @MappingTarget Transformation transformation);

    @Named("mapEndpointsToIds")
    default Set<Long> mapEndpointsToIds(Set<SourceSystemEndpoint> endpoints) {
        if (endpoints == null) {
            return Collections.emptySet();
        }
        return endpoints.stream()
                .map(endpoint -> endpoint.id)
                .collect(Collectors.toSet());
    }
}
