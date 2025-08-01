package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.EDCAccessPolicy;

public class EDCContractDefinitionMapper {

    public static EDCContractDefinitionDto toDto(EDCContractDefinition entity) {
        if (entity == null) {
            return null;
        }
        var dto = new EDCContractDefinitionDto();
        dto.setId(entity.id);
        dto.setContractDefinitionId(entity.contractDefinitionId);
        dto.setAssetId(entity.asset != null ? entity.asset.id : null);
        dto.setAccessPolicyId(entity.accessPolicy != null ? entity.accessPolicy.id : null);
        dto.setContractPolicyId(entity.contractPolicy != null ? entity.contractPolicy.id : null);
        return dto;
    }

    public static EDCContractDefinition fromDto(EDCContractDefinitionDto dto) {
        if (dto == null) {
            return null;
        }
        var entity = new EDCContractDefinition();
        // ID bleibt null bei create
        entity.contractDefinitionId = dto.getContractDefinitionId();
        // Beziehungen laden wir hier vorerst direkt mit Panache:
        if (dto.getAssetId() != null) {
            entity.asset = EDCAsset.findById(dto.getAssetId());
            if (entity.asset == null) {
                throw new IllegalArgumentException("Asset " + dto.getAssetId() + " nicht gefunden");
            }
        }
        if (dto.getAccessPolicyId() != null) {
            entity.accessPolicy = EDCAccessPolicy.findById(dto.getAccessPolicyId());
            if (entity.accessPolicy == null) {
                throw new IllegalArgumentException("AccessPolicy " + dto.getAccessPolicyId() + " nicht gefunden");
            }
        }
        if (dto.getContractPolicyId() != null) {
            entity.contractPolicy = EDCAccessPolicy.findById(dto.getContractPolicyId());
            if (entity.contractPolicy == null) {
                throw new IllegalArgumentException("ContractPolicy " + dto.getContractPolicyId() + " nicht gefunden");
            }
        }
        return entity;
    }
}
