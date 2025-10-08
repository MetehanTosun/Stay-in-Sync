package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "edc_contract_definition")
public class EDCContractDefinition extends UuidEntity {

    @Column(name = "contract_definition_id", unique = true, nullable = false)
    private String contractDefinitionId;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "asset_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    private Asset asset;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "access_policy_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    private PolicyDefinition accessPolicy;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "contract_policy_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    private PolicyDefinition contractPolicy;
    
    @ManyToOne
    @JoinColumn(name = "edc_instance_id")
    private EDCInstance edcInstance;

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

    public PolicyDefinition getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(PolicyDefinition accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public PolicyDefinition getContractPolicy() {
        return contractPolicy;
    }

    public void setContractPolicy(PolicyDefinition contractPolicy) {
        this.contractPolicy = contractPolicy;
    }
    
    public EDCInstance getEdcInstance() {
        return edcInstance;
    }
    
    public void setEdcInstance(EDCInstance edcInstance) {
        this.edcInstance = edcInstance;
    }
}
