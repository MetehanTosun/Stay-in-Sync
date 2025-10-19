package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "edc_contract_definition")
@NoArgsConstructor
public class EDCContractDefinition extends PanacheEntity {

    @Column(name = "contract_definition_id", unique = true, nullable = false)
    public String contractDefinitionId;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "asset_id",
        nullable = false
    )
    public EDCAsset asset;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "access_policy_id",
        nullable = false
    )
    public EDCPolicy accessPolicy;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "contract_policy_id",
        nullable = false
    )
    public EDCPolicy contractPolicy;
    
    @ManyToOne
    @JoinColumn(name = "edc_instance_id")
    public EDCInstance edcInstance;

    @Column(columnDefinition = "LONGTEXT")
    public String rawJson;

    public String getContractDefinitionId() {
        return contractDefinitionId;
    }

    public void setContractDefinitionId(String contractDefinitionId) {
        this.contractDefinitionId = contractDefinitionId;
    }

    public EDCAsset getAsset() {
        return asset;
    }

    public void setAsset(EDCAsset asset) {
        this.asset = asset;
    }

    public EDCPolicy getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(EDCPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public EDCPolicy getContractPolicy() {
        return contractPolicy;
    }

    public void setContractPolicy(EDCPolicy contractPolicy) {
        this.contractPolicy = contractPolicy;
    }
    
    public EDCInstance getEdcInstance() {
        return edcInstance;
    }
    
    public void setEdcInstance(EDCInstance edcInstance) {
        this.edcInstance = edcInstance;
    }
}
