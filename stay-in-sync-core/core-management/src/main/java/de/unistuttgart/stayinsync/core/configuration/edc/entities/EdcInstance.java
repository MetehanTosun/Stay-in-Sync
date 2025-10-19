package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor // für JPA & MapStruct
@Entity
@Table(name = "edc_instance")
public class EdcInstance extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(name = "control_plane_management_url")
    public String controlPlaneManagementUrl;

    public String protocolVersion;

    public String description;

    public String bpn;

    public String apiKey;

    public String edcAssetEndpoint;

    public String edcPolicyEndpoint;

    public String edcContractDefinitionEndpoint;

    /**
     * Die mit dieser EDC-Instanz verknüpften Policies.
     * Diese Liste enthält alle Policies, die für diese EDC-Instanz definiert sind.
     */
    @OneToMany(mappedBy = "edcInstance")
    public List<Policy> policies = new ArrayList<>();
    
    /**
     * Fügt eine Policy zu dieser EDC-Instanz hinzu.
     * 
     * @param policy Die hinzuzufügende Policy
     */
    public void addPolicy(Policy policy) {
        if (policies == null) {
            policies = new ArrayList<>();
        }
        policies.add(policy);
        policy.setEdcInstance(this);
    }
    
    /**
     * Entfernt eine Policy von dieser EDC-Instanz.
     * 
     * @param policy Die zu entfernende Policy
     * @return true, wenn die Policy erfolgreich entfernt wurde, sonst false
     */
    public boolean removePolicy(Policy policy) {
        if (policies != null && policies.remove(policy)) {
            policy.setEdcInstance(null);
            return true;
        }
        return false;
    }
}
