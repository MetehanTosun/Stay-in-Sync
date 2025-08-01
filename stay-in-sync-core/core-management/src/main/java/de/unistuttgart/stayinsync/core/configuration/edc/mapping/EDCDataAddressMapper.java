package de.unistuttgart.stayinsync.core.configuration.edc.mapping;


import de.unistuttgart.stayinsync.core.configuration.edc.EDCDataAddress;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;

public class EDCDataAddressMapper {

    public static EDCDataAddressDto toDto(EDCDataAddress entity) {
        if (entity == null) {
            return null;
        }
        // kein Chaining, sondern einzeln:
        EDCDataAddressDto dto = new EDCDataAddressDto();
        dto.setId(entity.id);
        dto.setJsonLDType(entity.jsonLDType);
        dto.setType(entity.type);
        dto.setBaseURL(entity.baseURL);
        dto.setProxyPath(entity.proxyPath);
        dto.setProxyQueryParams(entity.proxyQueryParams);
        return dto;
    }

    public static EDCDataAddress fromDto(EDCDataAddressDto dto) {
        if (dto == null) {
            return null;
        }
        EDCDataAddress e = new EDCDataAddress();
        e.setJsonLDType(dto.getJsonLDType());
        e.setType(dto.getType());
        e.setBaseURL(dto.getBaseURL());
        e.setProxyPath(dto.getProxyPath());
        e.setProxyQueryParams(dto.getProxyQueryParams());
        return e;
    }
}
