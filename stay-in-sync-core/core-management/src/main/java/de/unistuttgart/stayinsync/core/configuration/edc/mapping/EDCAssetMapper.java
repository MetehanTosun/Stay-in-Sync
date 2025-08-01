package de.unistuttgart.stayinsync.core.configuration.edc.mapping;


import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.*;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import java.util.stream.Collectors;


public class EDCAssetMapper {

    public static EDCAssetDto toDto(EDCAsset e) {
        if (e == null) return null;
        return new EDCAssetDto()
            .setId(e.id)
            .setAssetId(e.assetId)
            .setDataAddressId(e.dataAddress != null ? e.dataAddress.id : null)
            .setPropertiesId(e.properties != null ? e.properties.id : null)
            .setAccessPolicyIds(e.edcAccessPolicies != null
                ? e.edcAccessPolicies.stream().map(p -> p.id).collect(Collectors.toSet())
                : null)
            .setTargetSystemEndpointId(e.targetSystemEndpoint != null ? e.targetSystemEndpoint.id : null)
            .setTargetEDCId(e.targetEDC != null ? e.targetEDC.id : null);
    }

    public static EDCAsset fromDto(EDCAssetDto dto) {
        if (dto == null) return null;
        EDCAsset e = new EDCAsset();
        e.assetId = dto.getAssetId();

        // referenzierte Objekte laden
        EDCDataAddress addr = EDCDataAddress.findById(dto.getDataAddressId());
        if (addr == null) throw new IllegalArgumentException("DataAddress " + dto.getDataAddressId() + " nicht gefunden");
        e.dataAddress = addr;

        if (dto.getPropertiesId() != null) {
            EDCProperty prop = EDCProperty.findById(dto.getPropertiesId());
            if (prop == null) throw new IllegalArgumentException("Property " + dto.getPropertiesId() + " nicht gefunden");
            e.properties = prop;
        }

        if (dto.getAccessPolicyIds() != null) {
            // wir verknüpfen nur die IDs – Service schiebt ggf. die Policies drauf
            e.edcAccessPolicies = dto.getAccessPolicyIds().stream()
                 .map(id -> {
                  EDCAccessPolicy p = EDCAccessPolicy.findById(id);
             if (p == null) {
                throw new IllegalArgumentException("Policy " + id + " nicht gefunden");
             }
                 return p;
            })
                .collect(Collectors.toSet());

        }

        if (dto.getTargetSystemEndpointId() != null) {
            TargetSystemEndpoint tse = TargetSystemEndpoint.findById(dto.getTargetSystemEndpointId());
            if (tse == null) throw new IllegalArgumentException("Endpoint " + dto.getTargetSystemEndpointId() + " nicht gefunden");
            e.targetSystemEndpoint = tse;
        }

        EDC edc = EDC.findById(dto.getTargetEDCId());
        if (edc == null) throw new IllegalArgumentException("EDC " + dto.getTargetEDCId() + " nicht gefunden");
        e.targetEDC = edc;

        return e;
    }
}
