package de.unistuttgart.stayinsync.core.configuration.edc;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
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

    @OneToOne
    public TargetSystemEndpoint targetSystemEndpoint;

    @ManyToOne
    public EDC targetEDC;

    // Getter und Setter
    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public EDCDataAddress getDataAddress() {
        return dataAddress;
    }

    public void setDataAddress(EDCDataAddress dataAddress) {
        this.dataAddress = dataAddress;
    }

    public EDCProperty getProperties() {
        return properties;
    }

    public void setProperties(EDCProperty properties) {
        this.properties = properties;
    }

    public Set<EDCAccessPolicy> getEdcAccessPolicies() {
        return edcAccessPolicies;
    }

    public void setEdcAccessPolicies(Set<EDCAccessPolicy> edcAccessPolicies) {
        this.edcAccessPolicies = edcAccessPolicies;
    }

    public TargetSystemEndpoint getTargetSystemEndpoint() {
        return targetSystemEndpoint;
    }

    public void setTargetSystemEndpoint(TargetSystemEndpoint targetSystemEndpoint) {
        this.targetSystemEndpoint = targetSystemEndpoint;
    }

    public EDC getTargetEDC() {
        return targetEDC;
    }

    public void setTargetEDC(EDC targetEDC) {
        this.targetEDC = targetEDC;
    }

}
