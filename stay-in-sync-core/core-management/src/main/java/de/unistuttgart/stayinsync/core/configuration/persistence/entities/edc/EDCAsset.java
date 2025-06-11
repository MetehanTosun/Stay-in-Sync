package de.unistuttgart.stayinsync.core.configuration.persistence.entities.edc;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemEndpoint;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

@Entity
public class EDCAsset extends PanacheEntity {

    public String assetId;

    @OneToOne
    public EDCDataAddress dataAddress;

    @OneToOne
    public EDCProperty properties;

    @OneToMany(mappedBy = "edcAsset")
    public Set<EDCAccessPolicy> edcAccessPolicies;

    //TODO: Check if asset should have multiple TargetSystemEndpoints?
    @OneToOne
    public TargetSystemEndpoint targetSystemEndpoint;

    @ManyToOne
    public EDC targetEDC;
}
