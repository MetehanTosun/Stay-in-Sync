package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "edc_contract_definition")
@NoArgsConstructor
public class ContractDefinition extends PanacheEntity {

    @Column(name = "contract_definition_id", unique = true, nullable = false)
    public String contractDefinitionId;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "asset_id",
        nullable = false
    )
    public Asset asset;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "access_policy_id",
        nullable = false
    )
    public Policy accessPolicy;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "contract_policy_id",
        nullable = false
    )
    public Policy contractPolicy;
    
    @ManyToOne
    @JoinColumn(name = "edc_instance_id")
    public EdcInstance edcInstance;

    @Column(columnDefinition = "LONGTEXT")
    public String rawJson;

    public String getContractDefinitionId() {
        return contractDefinitionId;
    }

    public void setContractDefinitionId(String contractDefinitionId) {
        this.contractDefinitionId = contractDefinitionId;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Policy getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(Policy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public Policy getContractPolicy() {
        return contractPolicy;
    }

    public void setContractPolicy(Policy contractPolicy) {
        this.contractPolicy = contractPolicy;
    }
    
    public EdcInstance getEdcInstance() {
        return edcInstance;
    }
    
    public void setEdcInstance(EdcInstance edcInstance) {
        this.edcInstance = edcInstance;
    }
}
