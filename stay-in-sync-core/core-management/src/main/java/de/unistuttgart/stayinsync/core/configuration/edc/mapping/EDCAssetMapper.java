package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.EDCDataAddress;
import de.unistuttgart.stayinsync.core.configuration.edc.EDCProperty;
import de.unistuttgart.stayinsync.core.configuration.edc.EDC;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import java.util.Set;
import java.util.stream.Collectors;

public class EDCAssetMapper {

    public static EDCAssetDto toDto(EDCAsset asset) {
        if (asset == null) {
            return null;
        }
        EDCAssetDto dto = new EDCAssetDto()
            .setId(asset.id)
            .setAssetId(asset.assetId)
            .setDataAddressId(asset.dataAddress != null ? asset.dataAddress.id : null)
            .setPropertiesId(asset.properties != null ? asset.properties.id : null)
            .setTargetSystemEndpointId(
                asset.targetSystemEndpoint != null ? asset.targetSystemEndpoint.id : null
            )
            .setTargetEDCId(asset.targetEDC != null ? asset.targetEDC.id : null)
            .setAccessPolicyIds(extractPolicyIds(asset));
        return dto;
    }

    public static EDCAsset fromDto(EDCAssetDto dto) {
        if (dto == null) {
            return null;
        }
        EDCAsset asset = dto.getId() != null
            ? EDCAsset.findById(dto.getId())
            : new EDCAsset();

        asset.assetId = dto.getAssetId();
        asset.dataAddress = dto.getDataAddressId() != null
            ? EDCDataAddress.findById(dto.getDataAddressId())
            : null;
        asset.properties = dto.getPropertiesId() != null
            ? EDCProperty.findById(dto.getPropertiesId())
            : null;
        asset.targetSystemEndpoint = dto.getTargetSystemEndpointId() != null
            ? TargetSystemEndpoint.findById(dto.getTargetSystemEndpointId())
            : null;
        asset.targetEDC = dto.getTargetEDCId() != null
            ? EDC.findById(dto.getTargetEDCId())
            : null;
        // Access policies werden derzeit nicht gesetzt – geschieht über eigene Resource
        return asset;
    }

    private static Set<Long> extractPolicyIds(EDCAsset asset) {
        if (asset.edcAccessPolicies == null) {
            return Set.of();
        }
        return asset.edcAccessPolicies.stream()
            .map(p -> p.id)
            .collect(Collectors.toSet());
    }
}