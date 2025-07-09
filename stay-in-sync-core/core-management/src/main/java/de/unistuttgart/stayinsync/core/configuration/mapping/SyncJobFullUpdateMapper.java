package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Mapper to map all fields on an input {@link SyncJob} onto a target {@link SyncJob}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface SyncJobFullUpdateMapper {

    /**
     * Maps all fields except <code>id</code> from {@code input} onto {@code target}.
     *
     * @param input  The input {@link SyncJob}
     * @param target The target {@link SyncJob}
     */
    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(SyncJob input, @MappingTarget SyncJob target);

    SyncJobDTO mapToDTO(SyncJob input);

    List<SyncJobDTO> mapToDTOList(List<SyncJob> input);

    @Mapping(target = "id", source = "id")
    SyncJob mapToEntity(SyncJobDTO input);

    List<SyncJob> mapToEntityList(List<SyncJobDTO> input);
}
