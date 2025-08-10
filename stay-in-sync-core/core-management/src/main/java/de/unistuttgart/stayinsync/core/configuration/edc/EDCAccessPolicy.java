package de.unistuttgart.stayinsync.core.configuration.edc;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "edc_access_policy")
public class EDCAccessPolicy extends UuidEntity {

    @OneToMany(mappedBy = "edcAccessPolicy",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    public Set<EDCAccessPolicyPermission> accessPolicyPermissions;

    @ManyToOne
    @JoinColumn(name = "asset_id", columnDefinition = "CHAR(36)", nullable = false)
    public EDCAsset edcAsset;

    public Set<EDCAccessPolicyPermission> getAccessPolicyPermissions() {
        return accessPolicyPermissions;
    }

    public void setAccessPolicyPermissions(Set<EDCAccessPolicyPermission> accessPolicyPermissions) {
        this.accessPolicyPermissions = accessPolicyPermissions;
    }

    public EDCAsset getEdcAsset() {
        return edcAsset;
    }

    public void setEdcAsset(EDCAsset edcAsset) {
        this.edcAsset = edcAsset;
    }

    // Getter/Setter…
}
