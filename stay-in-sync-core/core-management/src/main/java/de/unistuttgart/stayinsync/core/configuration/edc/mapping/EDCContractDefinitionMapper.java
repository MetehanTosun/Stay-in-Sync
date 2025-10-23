package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Asset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.ContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Interface für den Mapper zwischen ContractDefinition und EDCContractDefinitionDto
 */
@Mapper(componentModel = "cdi")
public interface EDCContractDefinitionMapper {

    /**
     * Konvertiert ein ContractDefinition-Entity in ein EDCContractDefinitionDto
     */
    @Mapping(target = "assetId", source = "asset.assetId")
    @Mapping(target = "accessPolicyId", source = "accessPolicy.id")
    @Mapping(target = "contractPolicyId", source = "contractPolicy.id")
    @Mapping(target = "accessPolicyIdStr", ignore = true)
    @Mapping(target = "contractPolicyIdStr", ignore = true)
    EDCContractDefinitionDto toDto(ContractDefinition entity);
    
    /**
     * Statische Helfer-Methode für die Konvertierung von DTO zu Entity
     * Diese Methode wird direkt im Code referenziert
     */
    static ContractDefinition fromDto(EDCContractDefinitionDto dto) {
        if (dto == null) return null;
        
        ContractDefinition entity = new ContractDefinition();
        entity.id = dto.id();
        entity.contractDefinitionId = dto.contractDefinitionId() != null ? dto.contractDefinitionId() : "contract-" + System.currentTimeMillis();
        entity.rawJson = dto.rawJson();
        
        // Resolve asset by assetId if present
        if (dto.assetId() != null && !dto.assetId().isBlank()) {
            Asset asset = Asset.findByAssetId(dto.assetId());
            entity.asset = asset; // Kann null sein, wenn Asset nicht gefunden wird
        }

        // Set access policy based on Long ID or string id
        if (dto.accessPolicyId() != null) {
            entity.accessPolicy = Policy.findById(dto.accessPolicyId());
            if (entity.accessPolicy == null) {
                System.out.println("Access policy not found with ID: " + dto.accessPolicyId());
            }
        } else if (dto.accessPolicyIdStr() != null && !dto.accessPolicyIdStr().isBlank()) {
            entity.accessPolicy = Policy.findByPolicyId(dto.accessPolicyIdStr());
            if (entity.accessPolicy == null) {
                System.out.println("Access policy not found with policy ID: " + dto.accessPolicyIdStr());
            }
        }

        // Set contract policy based on Long ID or string id
        if (dto.contractPolicyId() != null) {
            entity.contractPolicy = Policy.findById(dto.contractPolicyId());
            if (entity.contractPolicy == null) {
                System.out.println("Contract policy not found with ID: " + dto.contractPolicyId());
            }
        } else if (dto.contractPolicyIdStr() != null && !dto.contractPolicyIdStr().isBlank()) {
            entity.contractPolicy = Policy.findByPolicyId(dto.contractPolicyIdStr());
            if (entity.contractPolicy == null) {
                System.out.println("Contract policy not found with policy ID: " + dto.contractPolicyIdStr());
            }
        }
        
        return entity;
    }
}
