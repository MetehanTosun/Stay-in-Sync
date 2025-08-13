package de.unistuttgart.stayinsync.monitoring.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public abstract class MonitoringGraphTransformationMapper {

    @Inject
    EntityManager em;

    public MonitoringTransformationDto mapToDto(Transformation entity) {
        if (entity == null) {
            Log.warn("MAPPER LOG: Source Transformation entity is null. Returning null DTO.");
            return null;
        }

        MonitoringTransformationDto dto = new MonitoringTransformationDto();
        dto.id = entity.id;
        dto.name = entity.name;
        dto.description = entity.description;

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

        return dto;
    }

    public Transformation mapToEntity(MonitoringTransformationDto dto) {
        if (dto == null) {
            Log.warn("MAPPER LOG: Source TransformationDTO is null. Returning null Entity.");
            return null;
        }

        Transformation entity = new Transformation();
        entity.id = dto.id;
        entity.name = dto.name;
        entity.description = dto.description;

        // SourceSystemApiRequestConfigurations setzen
        if (dto.sourceSystemIds != null && !dto.sourceSystemIds.isEmpty()) {
            entity.sourceSystemApiRequestConfigurations = dto.sourceSystemIds.stream()
                    .map(id -> em.find(SourceSystemApiRequestConfiguration.class, id))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        // TargetSystemApiRequestConfigurations setzen
        if (dto.targetSystemIds != null && !dto.targetSystemIds.isEmpty()) {
            entity.targetSystemApiRequestConfigurations = dto.targetSystemIds.stream()
                    .map(id -> em.find(TargetSystemApiRequestConfiguration.class, id))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        return entity;
    }
}
