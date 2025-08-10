package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;

public class EDCAssetMapper {

    private EDCAssetMapper() {
        // Utility-Klasse nicht instanziierbar
    }

    public static EDCAssetDto toDto(EDCAsset entity) {
        if (entity == null) {
            return null;
        }
        EDCAssetDto dto = new EDCAssetDto();
        dto.setId(entity.id);
        dto.setAssetId(entity.assetId);
        dto.setUrl(entity.url);
        dto.setType(entity.type);
        dto.setContentType(entity.contentType);
        dto.setDescription(entity.description);
        dto.setTargetEDCId(entity.targetEDC != null ? entity.targetEDC.id : null);
        return dto;
    }

    public static EDCAsset fromDto(EDCAssetDto dto) {
        if (dto == null) {
            return null;
        }
        EDCAsset entity = new EDCAsset();
        // ID: bei Update im Service setzen, hier bei Create überspringen
        entity.id = dto.getId();

        entity.assetId     = dto.getAssetId();
        entity.url         = dto.getUrl();
        entity.type        = dto.getType();
        entity.contentType = dto.getContentType();
        entity.description = dto.getDescription();

        // targetEDC laden und zuweisen
        EDCInstance edc = EDCInstance.findById(dto.getTargetEDCId());
        if (edc == null) {
            throw new IllegalArgumentException("EDCInstance mit ID " + dto.getTargetEDCId() + " nicht gefunden");
        }
        entity.targetEDC = edc;

        return entity;
    }
}
