package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
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
    EDCContractDefinitionDto toDto(EDCContractDefinition entity);

    /**
     * Manual conversion from DTO to entity
     */
    static EDCContractDefinition fromDto(EDCContractDefinitionDto dto) {
        if (dto == null) return null;
        
        var entity = new EDCContractDefinition();
        entity.id = dto.id();
        entity.contractDefinitionId = dto.contractDefinitionId();
        entity.rawJson = dto.rawJson();
        
        // Resolve asset by assetId if present
        if (dto.assetId() != null && !dto.assetId().isBlank()) {
            var asset = EDCAsset.findByAssetId(dto.assetId());
            entity.asset = asset;
        }

        // Set access policy based on Long ID or string id
        if (dto.accessPolicyId() != null) {
            entity.accessPolicy = EDCPolicy.findById(dto.accessPolicyId());
        } else if (dto.accessPolicyIdStr() != null && !dto.accessPolicyIdStr().isBlank()) {
            entity.accessPolicy = EDCPolicy.findByPolicyId(dto.accessPolicyIdStr());
        }

        // Set contract policy based on Long ID or string id
        if (dto.contractPolicyId() != null) {
            entity.contractPolicy = EDCPolicy.findById(dto.contractPolicyId());
        } else if (dto.contractPolicyIdStr() != null && !dto.contractPolicyIdStr().isBlank()) {
            entity.contractPolicy = EDCPolicy.findByPolicyId(dto.contractPolicyIdStr());
        }

        return entity;
    }
}
