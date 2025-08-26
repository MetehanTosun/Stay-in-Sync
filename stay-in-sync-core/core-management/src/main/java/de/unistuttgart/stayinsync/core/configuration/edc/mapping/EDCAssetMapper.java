package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCDataAddress;


public class EDCAssetMapper {

    private EDCAssetMapper() {
        // Utility-Klasse nicht instanziierbar
    }

    // ------------------ Entity -> DTO ------------------
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

        // DataAddress mappen
        if (entity.dataAddress != null) {
            EDCDataAddressDto da = new EDCDataAddressDto()
                    .setId(entity.dataAddress.id)
                    .setJsonLDType(entity.dataAddress.jsonLDType)
                    .setType(entity.dataAddress.type)
                    .setBaseURL(entity.dataAddress.baseURL)
                    .setProxyPath(entity.dataAddress.proxyPath)
                    .setProxyQueryParams(entity.dataAddress.proxyQueryParams);
            dto.setDataAddress(da);
        }

        // Properties mappen
        if (entity.properties != null) {
            EDCPropertyDto prop = new EDCPropertyDto()
                    .setId(entity.properties.id)
                    .setDescription(entity.properties.description);
            dto.setProperties(prop);
        }

        return dto;
    }

    // ------------------ DTO -> Entity ------------------
    public static EDCAsset fromDto(EDCAssetDto dto) {
        if (dto == null) {
            return null;
        }

        EDCAsset entity = new EDCAsset();
        // ID: bei Update im Service setzen, hier bei Create evtl. null lassen
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

        // DataAddress übernehmen
        if (dto.getDataAddress() != null) {
            EDCDataAddress da = new EDCDataAddress();
            da.id = dto.getDataAddress().getId();
            da.jsonLDType = dto.getDataAddress().getJsonLDType();
            da.type = dto.getDataAddress().getType();
            da.baseURL = dto.getDataAddress().getBaseURL();
            da.proxyPath = dto.getDataAddress().getProxyPath();
            da.proxyQueryParams = dto.getDataAddress().getProxyQueryParams();
            entity.dataAddress = da;
        }

        // Properties übernehmen
        if (dto.getProperties() != null) {
            EDCProperty prop = new EDCProperty();
            prop.id = dto.getProperties().getId();
            prop.description = dto.getProperties().getDescription();
            entity.properties = prop;
        }

        return entity;
    }
}
