package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EDCInstanceMapper {

    private EDCInstanceMapper() {
        // Utility-Klasse nicht instanziierbar
    }

    public static EDCInstanceDto toDto(EDCInstance entity) {
        if (entity == null) {
            return null;
        }
        EDCInstanceDto dto = new EDCInstanceDto();
        dto.setId(entity.id);
        dto.setName(entity.name);
        dto.setUrl(entity.url);
        dto.setApiKey(entity.apiKey);
        dto.setProtocolVersion(entity.protocolVersion);
        dto.setDescription(entity.description);
        dto.setBpn(entity.bpn);

        Set<UUID> assetIds = Optional.ofNullable(entity.edcAssets)
                .orElse(Set.of())
                .stream()
                .map(asset -> asset.id)
                .collect(Collectors.toUnmodifiableSet());
        dto.setEdcAssetIds(assetIds);

        return dto;
    }

    public static EDCInstance fromDto(EDCInstanceDto dto) {
        if (dto == null) {
            return null;
        }
        EDCInstance entity = (dto.getId() != null)
                ? EDCInstance.findById(dto.getId())
                : new EDCInstance();
        if (entity == null) {
            entity = new EDCInstance();
        }

        entity.id              = dto.getId();
        entity.name            = dto.getName();
        entity.url             = dto.getUrl();
        entity.apiKey          = dto.getApiKey();
        entity.protocolVersion = dto.getProtocolVersion();
        entity.description     = dto.getDescription();
        entity.bpn             = dto.getBpn();

        return entity;
    }
}
