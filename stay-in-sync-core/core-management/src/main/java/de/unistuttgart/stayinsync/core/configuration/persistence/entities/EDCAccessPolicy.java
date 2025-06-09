package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
public class EDCAccessPolicy extends PanacheEntity {
    @OneToMany
    public Set<EDCAccessPolicyPermission> accessPolicyPermissions;

    @ManyToOne
    public EDCAsset edcAsset;
}
