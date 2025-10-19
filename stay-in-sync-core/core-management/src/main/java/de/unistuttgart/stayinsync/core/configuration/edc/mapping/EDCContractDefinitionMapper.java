package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Asset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.ContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "cdi")
public interface EDCContractDefinitionMapper {

    EDCContractDefinitionMapper INSTANCE = Mappers.getMapper(EDCContractDefinitionMapper.class);

    @Mapping(target = "assetId", source = "asset.assetId")
    @Mapping(target = "accessPolicyId", source = "accessPolicy.id")
    @Mapping(target = "contractPolicyId", source = "contractPolicy.id")
    @Mapping(target = "accessPolicyIdStr", ignore = true)
    @Mapping(target = "contractPolicyIdStr", ignore = true)
    EDCContractDefinitionDto toDto(ContractDefinition entity);

    /**
     * Manual conversion from DTO to entity
     */
    static ContractDefinition fromDto(EDCContractDefinitionDto dto) {
        if (dto == null) return null;
        
        var entity = new ContractDefinition();
        entity.id = dto.id();
        entity.contractDefinitionId = dto.contractDefinitionId();
        entity.rawJson = dto.rawJson();
        
        // Resolve asset by assetId if present
        if (dto.assetId() != null && !dto.assetId().isBlank()) {
            var asset = Asset.findByAssetId(dto.assetId());
            entity.asset = asset;
        }

        // Set access policy based on Long ID or string id
        if (dto.accessPolicyId() != null) {
            entity.accessPolicy = Policy.findById(dto.accessPolicyId());
        } else if (dto.accessPolicyIdStr() != null && !dto.accessPolicyIdStr().isBlank()) {
            entity.accessPolicy = Policy.findByPolicyId(dto.accessPolicyIdStr());
        }

        // Set contract policy based on Long ID or string id
        if (dto.contractPolicyId() != null) {
            entity.contractPolicy = Policy.findById(dto.contractPolicyId());
        } else if (dto.contractPolicyIdStr() != null && !dto.contractPolicyIdStr().isBlank()) {
            entity.contractPolicy = Policy.findByPolicyId(dto.contractPolicyIdStr());
        }

        return entity;
    }
}
