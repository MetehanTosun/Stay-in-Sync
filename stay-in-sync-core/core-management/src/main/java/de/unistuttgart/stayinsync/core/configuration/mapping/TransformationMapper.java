package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationDetailsDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.transport.dto.TransformationMessageDTO;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, uses = {TransformationScriptMapper.class, SourceSystemApiRequestConfigurationFullUpdateMapper.class, TransformationRuleMapper.class})
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
    @Mapping(source = "transformationScript", target = "script")
    // Delegates to TransformationScriptMapper
    TransformationDetailsDTO mapToDetailsDTO(Transformation transformation);

    /**
     * Maps the Transformation entity to its deployable Message DTO.
     */
    @Mapping(source = "transformationScript", target = "transformationScriptDTO")
    @Mapping(source = "transformationRule", target = "transformationRuleDTO") // Will use TransformationRuleMapper
    @Mapping(source = "sourceSystemApiRequestConfigrations", target = "requestConfigurationMessageDTOS")
    @Mapping(source = "sourceSystemApiRequestConfigrations", target = "arcManifest", qualifiedByName = "buildArcManifest")
    TransformationMessageDTO mapToMessageDTO(Transformation transformation);

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

    @Named("buildArcManifest")
    default List<String> buildArcManifest(Set<SourceSystemApiRequestConfiguration> arcs){
        if (arcs == null || arcs.isEmpty()){
            return List.of();
        }
        return arcs.stream()
                .map(arc -> arc.alias)
                .toList();
    }
}
