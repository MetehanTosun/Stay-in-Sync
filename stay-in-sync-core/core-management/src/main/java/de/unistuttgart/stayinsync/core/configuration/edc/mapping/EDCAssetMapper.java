package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCDataAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(uses = {EDCDataAddressMapper.class, EDCPropertyMapper.class})
public interface EDCAssetMapper {

    EDCAssetMapper assetMapper = Mappers.getMapper(EDCAssetMapper.class);

    @Mapping(source = "targetEDC", target = "targetEDCId")
    EDCAssetDto assetToAssetDto(EDCAsset asset);

    @Mapping(source = "targetEDCId", target = "targetEDC")
    @Mapping(target = "targetSystemEndpoint", ignore = true)
    EDCAsset assetDtoToAsset(EDCAssetDto assetDto);

    default EDCInstance map(UUID targetEDCId) {
        if (targetEDCId == null) {
            return null;
        }
        return EDCInstance.findById(targetEDCId);
    }

    default UUID map(EDCInstance targetEDC) {
        if (targetEDC == null) {
            return null;
        }
        return targetEDC.id;
    }
}
