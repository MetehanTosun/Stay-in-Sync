package de.unistuttgart.stayinsync.core.configuration.edc;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "edc_contract_definition")
public class EDCContractDefinition extends UuidEntity {

    @Column(name = "contract_definition_id", unique = true, nullable = false)
    public String contractDefinitionId;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "asset_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    public EDCAsset asset;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "access_policy_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    public EDCAccessPolicy accessPolicy;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "contract_policy_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    public EDCAccessPolicy contractPolicy;

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

    public EDCAccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(EDCAccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public EDCAccessPolicy getContractPolicy() {
        return contractPolicy;
    }

    public void setContractPolicy(EDCAccessPolicy contractPolicy) {
        this.contractPolicy = contractPolicy;
    }
}
