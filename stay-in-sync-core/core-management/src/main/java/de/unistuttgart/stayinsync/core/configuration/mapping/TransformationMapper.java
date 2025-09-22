package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.RequestConfigurationMessageMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobTransformationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobTransformationRuleDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationDetailsDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.transport.dto.TransformationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import io.quarkus.logging.Log;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {
                TransformationScriptMapper.class,
                TransformationRuleMapper.class,
                SourceSystemApiRequestConfigurationFullUpdateMapper.class,
                RequestConfigurationMessageMapper.class})
public interface TransformationMapper {

    // TODO: Handle complex object mappings in service layer

    /**
     * Maps a complete Transformation entity to its detailed DTO representation.
     * This is used for all GET endpoints (e.g., get by ID, get all).
     */
    @Mapping(source = "syncJob.id", target = "syncJobId")
    @Mapping(source = "transformationRule.id", target = "transformationRule.id")
    @Mapping(source = "transformationRule.name", target = "transformationRule.name")
    @Mapping(source = "transformationScript", target = "script")
    // Delegates to TransformationScriptMapper
    TransformationDetailsDTO mapToDetailsDTO(Transformation transformation);

    /**
     * Maps the Transformation entity to its deployable Message DTO.
     */
    @Mapping(source = "transformationScript", target = "transformationScriptDTO")
    @Mapping(source = "transformationRule", target = "transformationRuleDTO")
    @Mapping(source = "sourceSystemApiRequestConfigurations", target = "requestConfigurationMessageDTOS")
    @Mapping(source = "targetSystemApiRequestConfigurations", target = "targetRequestConfigurationMessageDTOS")
    @Mapping(source = "sourceSystemApiRequestConfigurations", target = "arcManifest", qualifiedByName = "buildSourceArcManifest")
    TransformationMessageDTO mapToMessageDTO(Transformation transformation);

    /**
     * Updates a new Transformation entity from a TransformationShellDTO.
     * This is used by the 'createShell' service method. It only sets the initial fields
     * and ignores all relationships, which will be assembled later.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "syncJob", ignore = true)
    @Mapping(target = "transformationRule", ignore = true)
    @Mapping(target = "transformationScript", ignore = true)
    @Mapping(target = "sourceSystemVariables", ignore = true)
    void updateFromShellDTO(TransformationShellDTO dto, @MappingTarget Transformation transformation);

    @Mapping(target = "name", source = "transformationScript.name")
    SyncJobTransformationDTO mapToSyncJobDTO(Transformation transformation);

    SyncJobTransformationRuleDTO mapToSyncJobDTO(TransformationRule transformationRule);


    @Named("mapEndpointsToIds")
    default Set<Long> mapEndpointsToIds(Set<SourceSystemEndpoint> endpoints) {
        if (endpoints == null) {
            return Collections.emptySet();
        }
        return endpoints.stream()
                .map(endpoint -> endpoint.id)
                .collect(Collectors.toSet());
    }

    @Named("buildSourceArcManifest")
    default List<String> buildArcManifest(Set<SourceSystemApiRequestConfiguration> arcs){
        if (arcs == null || arcs.isEmpty()){
            return List.of();
        }
        return arcs.stream()
                .map(arc -> arc.alias)
                .toList();
    }

     default MonitoringTransformationDto mapToMonitoringGraphDto(Transformation entity) {
        if (entity == null) {
            Log.warn("MAPPER LOG: Source Transformation entity is null. Returning null DTO.");
            return null;
        }

        MonitoringTransformationDto dto = new MonitoringTransformationDto();
        dto.id = entity.id;
        dto.name = entity.name;
        dto.description = entity.description;
        dto.error = false;

         Log.info("SourceSystemApiRequestConfigurations size: " +
                 (entity.sourceSystemApiRequestConfigurations != null ? entity.sourceSystemApiRequestConfigurations.size() : "null"));

         Log.info("TargetSystemApiRequestConfigurations size: " +
                 (entity.targetSystemApiRequestConfigurations != null ? entity.targetSystemApiRequestConfigurations.size() : "null"));


         Log.info("MAPPER LOG: Transformation entity is " + entity);

        dto.sourceSystemIds = entity.sourceSystemApiRequestConfigurations != null
                ? entity.sourceSystemApiRequestConfigurations.stream()
                .map(cfg -> cfg.sourceSystem != null ? cfg.sourceSystem.id : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                : List.of();

        dto.targetSystemIds = entity.targetSystemApiRequestConfigurations != null
                ? entity.targetSystemApiRequestConfigurations.stream()
                .map(cfg -> cfg.targetSystem != null ? cfg.targetSystem.id : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                : List.of();

        dto.pollingNodes = entity.sourceSystemApiRequestConfigurations != null
                ? entity.sourceSystemApiRequestConfigurations.stream()
                .map(cfg -> cfg.workerPodName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                : List.of();

        return dto;
    }
}
