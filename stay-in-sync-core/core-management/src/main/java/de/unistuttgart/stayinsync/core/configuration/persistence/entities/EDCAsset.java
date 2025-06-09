package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

@Entity
public class EDCAsset extends PanacheEntity {

    public String assetId;
    @ManyToOne
    public EDC targetEDC;

    @OneToOne
    public EDCDataAddress dataAddress;
    @OneToOne
    public EDCProperty properties;

    @OneToMany
    public Set<EDCAccessPolicy> edcAccessPolicies;

    //TODO: Check if asset should have multiple TargetSystemEndpoints?
    @OneToOne
    public TargetSystemEndpoint targetSystemEndpoint;
}
