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
            .setId(entity.id)
            .setContractDefinitionId(entity.getContractDefinitionId())
            .setAssetId(entity.getAsset() != null ? entity.getAsset().getAssetId() : null)
            .setAccessPolicyId(entity.getAccessPolicy() != null ? entity.getAccessPolicy().id : null)
            .setContractPolicyId(entity.getContractPolicy() != null ? entity.getContractPolicy().id : null);
        return dto;
    }

    public static EDCContractDefinition fromDto(EDCContractDefinitionDto dto) {
        if (dto == null) return null;
        var entity = new EDCContractDefinition();
        entity.id                   = dto.getId(); // leer bei create, gesetzt bei update
        entity.setContractDefinitionId(dto.getContractDefinitionId());
        entity.setAsset(EDCAsset.findByAssetId(dto.getAssetId()));
        
        // Set access policy based on ID
        if (dto.getAccessPolicyId() != null) {
            entity.setAccessPolicy(EDCPolicy.findById(dto.getAccessPolicyId()));
        } else if (dto.getAccessPolicyIdStr() != null && !dto.getAccessPolicyIdStr().isEmpty()) {
            // Try to find by string ID if UUID is not provided
            entity.setAccessPolicy(EDCPolicy.findByPolicyId(dto.getAccessPolicyIdStr()));
        }
        
        // Set contract policy based on ID
        if (dto.getContractPolicyId() != null) {
            entity.setContractPolicy(EDCPolicy.findById(dto.getContractPolicyId()));
        } else if (dto.getContractPolicyIdStr() != null && !dto.getContractPolicyIdStr().isEmpty()) {
            // Try to find by string ID if UUID is not provided
            entity.setContractPolicy(EDCPolicy.findByPolicyId(dto.getContractPolicyIdStr()));
        }
        
        return entity;
    }
}
