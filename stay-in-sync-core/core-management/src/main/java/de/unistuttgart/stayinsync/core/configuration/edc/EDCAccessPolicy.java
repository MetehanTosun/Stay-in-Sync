package de.unistuttgart.stayinsync.core.configuration.edc;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.Set;

@Entity
public class EDCAccessPolicy extends PanacheEntity {

    @OneToMany(mappedBy = "edcAccessPolicy",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    public Set<EDCAccessPolicyPermission> accessPolicyPermissions;

    @ManyToOne
    public EDCAsset edcAsset;

    // --- Getter & Setter ---
    public Set<EDCAccessPolicyPermission> getAccessPolicyPermissions() {
        return accessPolicyPermissions;
    }

    public void setAccessPolicyPermissions(Set<EDCAccessPolicyPermission> perms) {
        this.accessPolicyPermissions = perms;
        if (perms != null) {
            perms.forEach(p -> p.setEdcAccessPolicy(this));
        }
    }

    public EDCAsset getEdcAsset() {
        return edcAsset;
    }

    public void setEdcAsset(EDCAsset edcAsset) {
        this.edcAsset = edcAsset;
    }
}
