package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.ContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.ContractDefinitionMapper;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "edc_contract_definition")
public class ContractDefinition extends PanacheEntity {

    private String contractDefinitionId;
    private String accessPolicyId;
    private String contractPolicyId;
    private String assetSelector;
    private boolean entityOutOfSync = false;
    @ManyToOne
    @JoinColumn(name = "edc_instance")
    private EDCInstance targetEdc;



    public void updateValuesWithContractDefinitionDto(final ContractDefinitionDto dto){
        this.setContractDefinitionId(dto.contractDefinitionId());
        this.setAccessPolicyId(dto.accessPolicyId());
        this.setContractPolicyId(dto.contractPolicyId());
        this.setAssetSelector(
                ContractDefinitionMapper.mapper.assetSelectorMapToString(dto.assetsSelector())
        );
    }
}
