package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;


/**
 * Mapper to map <code><strong>non-null</strong></code> fields on an input {@link SyncJob} onto a target {@link SyncJob}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, nullValuePropertyMappingStrategy = IGNORE)
public interface SyncJobPartialUpdateMapper {

    void mapPartialUpdate(SyncJob input, @MappingTarget SyncJob target);

}
