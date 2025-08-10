package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.*;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;

import java.util.UUID;

public class EDCContractDefinitionMapper {

    private EDCContractDefinitionMapper(){}

    public static EDCContractDefinitionDto toDto(EDCContractDefinition entity) {
        if (entity == null) return null;
        var dto = new EDCContractDefinitionDto()
            .setId(entity.id)
            .setContractDefinitionId(entity.contractDefinitionId)
            .setAssetId(entity.asset != null ? entity.asset.id : null)
            .setAccessPolicyId(entity.accessPolicy != null ? entity.accessPolicy.id : null)
            .setContractPolicyId(entity.contractPolicy != null ? entity.contractPolicy.id : null);
        return dto;
    }

    public static EDCContractDefinition fromDto(EDCContractDefinitionDto dto) {
        if (dto == null) return null;
        var entity = new EDCContractDefinition();
        entity.id                   = dto.getId(); // leer bei create, gesetzt bei update
        entity.contractDefinitionId = dto.getContractDefinitionId();
        entity.asset                = EDCAsset.findById(dto.getAssetId());
        entity.accessPolicy         = EDCAccessPolicy.findById(dto.getAccessPolicyId());
        entity.contractPolicy       = EDCAccessPolicy.findById(dto.getContractPolicyId());
        return entity;
    }
}
