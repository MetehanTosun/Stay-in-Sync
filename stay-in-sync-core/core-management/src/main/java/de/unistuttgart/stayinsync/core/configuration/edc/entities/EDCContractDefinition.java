package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import java.util.UUID;

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
    public EDCPolicy accessPolicy;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "contract_policy_id",
        columnDefinition = "CHAR(36)",
        nullable = false
    )
    public EDCPolicy contractPolicy;

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

    public UUID getId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getId'");
    }

    public void setId(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setId'");
    }
}
