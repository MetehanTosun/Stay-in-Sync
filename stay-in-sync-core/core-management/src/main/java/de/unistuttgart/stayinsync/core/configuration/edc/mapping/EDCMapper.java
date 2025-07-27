package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.EDC;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDto;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

public class EDCMapper {

    private EDCMapper() {}

    public static EDCDto toDto(EDC edc) {
        if (edc == null) return null;
        EDCDto dto = new EDCDto()
            .setId(edc.id)
            .setName(edc.name)
            .setUrl(edc.url)
            .setApiKey(edc.apiKey);

        dto.setEdcAssetIds(
            Optional.ofNullable(edc.edcAssets)
                    .orElse(Set.of())
                    .stream()
                    .map(a -> a.id)
                    .collect(Collectors.toUnmodifiableSet())
        );
        return dto;
    }

    public static EDC fromDto(EDCDto dto) {
        if (dto == null) return null;
        EDC edc = dto.getId() != null
            ? EDC.findById(dto.getId())
            : new EDC();
        edc.name   = dto.getName();
        edc.url    = dto.getUrl();
        edc.apiKey = dto.getApiKey();
        // Assets werden nicht hier gesetzt – über EDCAssetResource verwaltet
        return edc;
    }
}
