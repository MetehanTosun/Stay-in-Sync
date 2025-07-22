package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateEDCAssetDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.EDCAssetDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface EDCAssetMapper {

    EDCAssetDTO mapToDTO(EDCAsset input);

    List<EDCAssetDTO> mapToDTOList(List<EDCAsset> input);

    EDCAsset mapToEntity(CreateEDCAssetDTO input);
}
