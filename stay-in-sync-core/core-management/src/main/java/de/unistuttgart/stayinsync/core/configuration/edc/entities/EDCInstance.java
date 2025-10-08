package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor // für JPA & MapStruct
@Entity
@Table(name = "edc_instance")
public class EDCInstance extends UuidEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "control_plane_management_url")
    private String controlPlaneManagementUrl;

    private String protocolVersion;

    private String description;

    private String bpn;

    private String apiKey;

    private String edcAssetEndpoint;

    private String edcPolicyEndpoint;

    private String edcContractDefinitionEndpoint;

    /**
     * Die mit dieser EDC-Instanz verknüpften Policies.
     * Diese Liste enthält alle Policies, die für diese EDC-Instanz definiert sind.
     */
    @OneToMany(mappedBy = "edcInstance")
    private List<PolicyDefinition> policies = new ArrayList<>();
    
    /**
     * Fügt eine Policy zu dieser EDC-Instanz hinzu.
     * 
     * @param policy Die hinzuzufügende Policy
     */
    public void addPolicy(PolicyDefinition policy) {
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
    public boolean removePolicy(PolicyDefinition policy) {
        if (policies != null && policies.remove(policy)) {
            policy.setEdcInstance(null);
            return true;
        }
        return false;
    }
}
