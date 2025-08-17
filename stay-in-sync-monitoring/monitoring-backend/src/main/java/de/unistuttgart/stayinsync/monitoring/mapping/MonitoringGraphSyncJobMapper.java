package de.unistuttgart.stayinsync.monitoring.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSyncJobDto;
import jakarta.inject.Inject;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.stream.Collectors;


@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public abstract class MonitoringGraphSyncJobMapper {

    @Inject
    MonitoringGraphTransformationMapper transformationMapper;

    public MonitoringSyncJobDto mapToDto(SyncJob entity) {
        MonitoringSyncJobDto dto = new MonitoringSyncJobDto();
        dto.id = entity.id;
        dto.name = entity.name;
        dto.deployed = entity.deployed;

        if (entity.transformations != null) {
            dto.transformations = entity.transformations.stream()
                    .map(transformationMapper::mapToDto)
                    .collect(Collectors.toList());
        } else {
            dto.transformations = List.of();
        }

        return dto;
    }

}
