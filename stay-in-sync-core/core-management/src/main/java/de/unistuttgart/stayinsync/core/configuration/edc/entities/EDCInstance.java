package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

}
