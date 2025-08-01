package de.unistuttgart.stayinsync.core.configuration.edc;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;

@Entity
public class EDCContractDefinition extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String contractDefinitionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    public EDCAsset asset;

    @ManyToOne(optional = false)
    @JoinColumn(name = "access_policy_id", nullable = false)
    public EDCAccessPolicy accessPolicy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contract_policy_id", nullable = false)
    public EDCAccessPolicy contractPolicy;
}
