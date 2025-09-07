package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSyncJobDto;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.stream.Collectors;


@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public abstract class MonitoringGraphSyncJobMapper {

    @Inject
    TransformationMapper transformationMapper;

    public List<MonitoringSyncJobDto> mapToDto(List<SyncJob> entities) {
        return entities.stream().map(entity -> {
            MonitoringSyncJobDto dto = new MonitoringSyncJobDto();
            dto.id = entity.id;
            dto.name = entity.name;
            dto.isSimulation = entity.isSimulation;

            Log.info("MAPPER LOG: Transformation entity is " + entity.transformations);

            if (entity.transformations != null) {
                dto.transformations = entity.transformations.stream()
                        .map(transformationMapper::mapToMonitoringGraphDto)
                        .collect(Collectors.toList());
            } else {
                dto.transformations = List.of();
            }

            return dto;
        }).collect(Collectors.toList());
    }

}
