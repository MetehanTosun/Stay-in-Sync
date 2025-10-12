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
    private EDCAsset asset;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "access_policy_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    private EDCPolicy accessPolicy;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "contract_policy_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    private EDCPolicy contractPolicy;
    
    @ManyToOne
    @JoinColumn(name = "edc_instance_id")
    private EDCInstance edcInstance;

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
