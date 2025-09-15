package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;

public class EDCContractDefinitionMapper {

    private EDCContractDefinitionMapper(){}

    public static EDCContractDefinitionDto toDto(EDCContractDefinition entity) {
        if (entity == null) return null;
        var dto = new EDCContractDefinitionDto()
            .setId(entity != null ? entity.id : null)
            .setContractDefinitionId(entity != null ? entity.getContractDefinitionId() : null)
            .setAssetId(entity != null && entity.getAsset() != null ? entity.getAsset().getAssetId() : null)
            .setAccessPolicyId(entity != null && entity.getAccessPolicy() != null ? entity.getAccessPolicy().id : null)
            .setContractPolicyId(entity != null && entity.getContractPolicy() != null ? entity.getContractPolicy().id : null);
        return dto;
    }

    public static EDCContractDefinition fromDto(EDCContractDefinitionDto dto) {
        if (dto == null) return null;
        var entity = new EDCContractDefinition();
        entity.id = dto.getId(); // leer bei create, gesetzt bei update
        entity.setContractDefinitionId(dto.getContractDefinitionId());

        // Resolve asset by assetId if present
        if (dto.getAssetId() != null && !dto.getAssetId().isBlank()) {
            var asset = EDCAsset.findByAssetId(dto.getAssetId());
            entity.setAsset(asset);
        }

        // Set access policy based on UUID or string id
        if (dto.getAccessPolicyId() != null) {
            entity.setAccessPolicy(EDCPolicy.findById(dto.getAccessPolicyId()));
        } else if (dto.getAccessPolicyIdStr() != null && !dto.getAccessPolicyIdStr().isBlank()) {
            entity.setAccessPolicy(EDCPolicy.findByPolicyId(dto.getAccessPolicyIdStr()));
        }

        // Set contract policy based on UUID or string id
        if (dto.getContractPolicyId() != null) {
            entity.setContractPolicy(EDCPolicy.findById(dto.getContractPolicyId()));
        } else if (dto.getContractPolicyIdStr() != null && !dto.getContractPolicyIdStr().isBlank()) {
            entity.setContractPolicy(EDCPolicy.findByPolicyId(dto.getContractPolicyIdStr()));
        }

        return entity;
    }
}
